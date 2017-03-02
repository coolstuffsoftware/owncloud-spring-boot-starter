/*
   Copyright (C) 2017 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl.local;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceWatchServiceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.MessageDigestAlgorithm;

/**
 * @author mufasa1976
 *
 */
abstract class AbstractOwncloudLocalResourceChecksumServiceImpl implements OwncloudLocalResourceChecksumServiceWithListenerRegistration {

  private MessageDigest messageDigest;

  @Autowired
  private OwncloudLocalProperties properties;

  private final Map<String, Consumer<Path>> changeListeners = new HashMap<>();
  private final Map<String, Consumer<Path>> deleteListeners = new HashMap<>();

  private Map<Path, String> checksums = new ConcurrentHashMap<>();

  @PostConstruct
  public void afterPropertiesSetOnAbstractClass() throws Exception {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    MessageDigestAlgorithm messageDigestAlgorithm = resourceProperties.getMessageDigestAlgorithm();
    Validate.notNull(messageDigestAlgorithm);
    messageDigest = messageDigestAlgorithm.getMessageDigest();
  }

  protected ResourceServiceProperties getResourceProperties() {
    return properties.getResourceService();
  }

  protected Collection<Consumer<Path>> getChangeListeners() {
    return changeListeners.values();
  }

  protected Collection<Consumer<Path>> getDeleteListeners() {
    return deleteListeners.values();
  }

  protected Map<Path, String> getChecksums() {
    return checksums;
  }

  @Override
  public String getChecksum(Path path) throws OwncloudResourceException {
    return Optional.ofNullable(path)
        .map(p -> checksums.get(p))
        .orElse(null);
  }

  @Override
  public void setChecksum(Path path, String checksum) throws OwncloudResourceException {
    Optional.ofNullable(path)
        .ifPresent(p -> {
          Validate.notBlank(checksum);
          checksums.put(p.toAbsolutePath().normalize(), checksum);
        });
  }

  @Override
  public synchronized String registerChangeListener(Consumer<Path> listener) {
    String id = createListenerId();
    changeListeners.put(id, listener);
    return id;
  }

  private String createListenerId() {
    return UUID.randomUUID().toString();
  }

  @Override
  public synchronized String registerDeleteListener(Consumer<Path> listener) {
    String id = createListenerId();
    deleteListeners.put(id, listener);
    return id;
  }

  @Override
  public synchronized void deregisterChangeListener(String id) {
    Validate.notBlank(id);
    changeListeners.remove(id);
  }

  @Override
  public synchronized void deregisterDeleteListener(String id) {
    Validate.notBlank(id);
    deleteListeners.remove(id);
  }

  @Override
  public synchronized void clearChangeListener() {
    changeListeners.clear();
  }

  @Override
  public synchronized void clearDeleteListener() {
    deleteListeners.clear();
  }

  protected String createDirectoryChecksum(Path path, Map<Path, String> fileChecksums) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      for (Entry<Path, String> checksumEntry : fileChecksums.entrySet()) {
        Path filePath = checksumEntry.getKey();
        String fileChecksum = checksumEntry.getValue();
        if (Files.isSameFile(path, filePath.getParent())) {
          stream.write(fileChecksum.getBytes());
        }
      }
      synchronized (messageDigest) {
        messageDigest.reset();
        messageDigest.update(stream.toByteArray());
        return Hex.encodeHexString(messageDigest.digest());
      }
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  protected String createFileChecksum(Path path) {
    try (InputStream stream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
      synchronized (messageDigest) {
        messageDigest.reset();
        byte[] buffer = IOUtils.toByteArray(stream);
        messageDigest.update(buffer);
        return Hex.encodeHexString(messageDigest.digest());
      }
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  @Slf4j
  protected static class InitializingFileVisitor extends SimpleFileVisitor<Path> {

    private final WatchService watchService;
    private final Function<Path, String> fileDigest;
    private final BiFunction<Path, Map<Path, String>, String> directoryDigest;

    @Getter
    private final Map<Path, String> checksums;

    @Getter
    @Setter
    private boolean stopVisitor = false;

    @Builder
    private InitializingFileVisitor(
        final WatchService watchService,
        final Function<Path, String> fileDigest,
        final BiFunction<Path, Map<Path, String>, String> directoryDigest,
        final Map<Path, String> checksums) {
      this.watchService = watchService;
      this.fileDigest = fileDigest;
      this.directoryDigest = directoryDigest;
      this.checksums = Optional.ofNullable(checksums)
          .orElse(new HashMap<>());
    }

    public void clearChecksums() {
      checksums.clear();
    }

    public String getChecksum(Path path) {
      return checksums.get(path);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (isStopVisitor()) {
        return FileVisitResult.TERMINATE;
      }
      return FileVisitResult.TERMINATE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (isStopVisitor()) {
        return FileVisitResult.TERMINATE;
      }
      log.info("Calculate the Checksum of File {}", file);
      String checksum = fileDigest.apply(file);
      checksums.put(file.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (isStopVisitor()) {
        return FileVisitResult.TERMINATE;
      }
      registerPathToWatchService(dir);
      String checksum = directoryDigest.apply(dir, checksums);
      checksums.put(dir.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

    private void registerPathToWatchService(Path dir) throws IOException {
      if (watchService != null) {
        dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      }
    }

  }

}
