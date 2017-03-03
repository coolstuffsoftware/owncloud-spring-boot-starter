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
import org.apache.commons.collections4.MapUtils;
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
@Slf4j
abstract class AbstractOwncloudLocalResourceChecksumServiceImpl implements OwncloudLocalResourceChecksumService {

  private MessageDigest messageDigest;

  @Autowired
  private OwncloudLocalProperties properties;

  private Map<Path, String> checksums = new ConcurrentHashMap<>();

  @PostConstruct
  public void afterPropertiesSetOnAbstractClass() throws Exception {
    log.debug("Using ChecksumService Strategy {} (Implementation-Class: {})", getStrategy(), getClass());
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    MessageDigestAlgorithm messageDigestAlgorithm = resourceProperties.getMessageDigestAlgorithm();
    Validate.notNull(messageDigestAlgorithm);
    messageDigest = messageDigestAlgorithm.getMessageDigest();
  }

  protected ResourceServiceProperties getResourceProperties() {
    return properties.getResourceService();
  }

  protected Map<Path, String> getChecksums() {
    return checksums;
  }

  protected boolean notContainsKey(Path path) {
    return !containsKey(path);
  }

  protected boolean containsKey(Path path) {
    return checksums.containsKey(path.toAbsolutePath().normalize());
  }

  protected void setChecksum(Path path, String checksum) {
    checksums.put(path.toAbsolutePath().normalize(), checksum);
  }

  protected void removeChecksum(Path path) {
    checksums.remove(path.toAbsolutePath().normalize());
  }

  @Override
  public String getChecksum(Path path) throws OwncloudResourceException {
    return Optional.ofNullable(path)
        .map(p -> checksums.get(p.toAbsolutePath().normalize()))
        .orElse(null);
  }

  @Override
  public void recalculateChecksum(Path path) throws OwncloudResourceException {
    Optional.ofNullable(path)
        .ifPresent(p -> {
          String checksum = createChecksum(p);
          setChecksum(p, checksum);
        });
  }

  protected String createChecksum(Path path) {
    if (Files.isDirectory(path)) {
      return createDirectoryChecksum(path, checksums);
    }
    return createFileChecksum(path);
  }

  protected String createDirectoryChecksum(Path path, Map<Path, String> fileChecksums) {
    log.debug("Calculate the Checksum of Directory {}", path);
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
    log.debug("Calculate the Checksum of File {}", path);
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

  protected String createListenerId() {
    return UUID.randomUUID().toString();
  }

  protected static class InitializingFileVisitor extends SimpleFileVisitor<Path> {

    @Setter
    private WatchService watchService;
    private final Function<Path, String> fileDigest;
    private final BiFunction<Path, Map<Path, String>, String> directoryDigest;

    private final Map<String, Consumer<Path>> refreshListeners;

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
        final Map<Path, String> checksums,
        final Map<String, Consumer<Path>> refreshListeners) {
      this.watchService = watchService;
      this.fileDigest = fileDigest;
      this.directoryDigest = directoryDigest;
      this.checksums = Optional.ofNullable(checksums)
          .orElse(new HashMap<>());
      this.refreshListeners = refreshListeners;
    }

    @SuppressWarnings("unused")
    public static class InitializingFileVisitorBuilder {
      private boolean stopVisitor = false;
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
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (isStopVisitor()) {
        return FileVisitResult.TERMINATE;
      }
      String checksum = fileDigest.apply(file);
      checksums.put(file.toAbsolutePath().normalize(), checksum);
      fireRefreshListeners(file);
      return FileVisitResult.CONTINUE;
    }

    private void fireRefreshListeners(Path path) {
      if (MapUtils.isNotEmpty(refreshListeners)) {
        refreshListeners.values().stream()
            .forEach(listener -> listener.accept(path));
      }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      if (isStopVisitor()) {
        return FileVisitResult.TERMINATE;
      }
      registerPathToWatchService(dir);
      String checksum = directoryDigest.apply(dir, checksums);
      checksums.put(dir.toAbsolutePath().normalize(), checksum);
      fireRefreshListeners(dir);
      return FileVisitResult.CONTINUE;
    }

    private void registerPathToWatchService(Path dir) throws IOException {
      if (watchService != null) {
        dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      }
    }

  }

}
