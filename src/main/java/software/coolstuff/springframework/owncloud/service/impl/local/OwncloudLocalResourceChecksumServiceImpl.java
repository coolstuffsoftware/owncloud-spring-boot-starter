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
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Builder;
import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceWatchServiceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.FileWatcherThreadProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.MessageDigestAlgorithm;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudLocalResourceChecksumServiceImpl implements OwncloudLocalResourceChecksumService, Runnable {

  @Autowired
  private OwncloudLocalProperties properties;

  private Thread fileWatcherThread;
  private MessageDigest messageDigest;

  private final List<Consumer<Path>> changeListeners = new ArrayList<>();
  private final List<Consumer<Path>> deleteListeners = new ArrayList<>();

  private final Map<Path, String> checksums = new HashMap<>();

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(resourceProperties.getLocation());

    MessageDigestAlgorithm messageDigestAlgorithm = resourceProperties.getMessageDigestAlgorithm();
    Validate.notNull(messageDigestAlgorithm);
    messageDigest = messageDigestAlgorithm.getMessageDigest();

    FileWatcherThreadProperties fileWatcherThreadProperties = resourceProperties.getFileWatcherThread();
    fileWatcherThread = new Thread(this);
    fileWatcherThread.setName(fileWatcherThreadProperties.getName());
    fileWatcherThread.setPriority(fileWatcherThreadProperties.getPriority());
    fileWatcherThread.start();
  }

  @PreDestroy
  public void destroy() throws Exception {
    if (fileWatcherThread != null && fileWatcherThread.isAlive()) {
      log.info("Shutting down FileWatcher Thread {}", fileWatcherThread.getName());
      fileWatcherThread.interrupt();
      fileWatcherThread.join();
    }
  }

  @Override
  public void run() {
    log.info("Starting FileWatcher Thread {}", Thread.currentThread().getName());
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    try (WatchService watchService = resourceProperties.getLocation().getFileSystem().newWatchService()) {
      registerExistingDirectories(watchService);
      watchDirectoryChanges(watchService);
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
    log.info("FileWatcher Thread {} has been shut down", Thread.currentThread().getName());
  }

  private void registerExistingDirectories(WatchService watchService) {
    try {
      ResourceServiceProperties resourceProperties = properties.getResourceService();
      checksums.clear();
      val fileVisitor = InitializingFileVisitor.builder()
          .watchService(watchService)
          .fileDigest(this::createFileChecksum)
          .directoryDigest(this::createDirectoryChecksum)
          .build();
      log.debug("Get the Checksums of all Files and Directories under Path {}", resourceProperties.getLocation());
      Files.walkFileTree(resourceProperties.getLocation(), fileVisitor);
      checksums.putAll(fileVisitor.getChecksums());
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  private String createChecksum(Path path) {
    if (Files.isDirectory(path)) {
      return createDirectoryChecksum(path, checksums);
    }
    return createFileChecksum(path);
  }

  private String createDirectoryChecksum(Path path, Map<Path, String> checksums) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      for (Entry<Path, String> checksumEntry : checksums.entrySet()) {
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

  private String createFileChecksum(Path path) {
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

  private void watchDirectoryChanges(WatchService watchService) {
    for (;;) {
      try {
        WatchKey watchKey = watchService.take();
        if (watchKey != null && watchKey.isValid()) {
          performFileChange(watchKey, watchService);
          watchKey.reset();
        }
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  private void performFileChange(WatchKey watchKey, WatchService watchService) {
    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
      if (ENTRY_CREATE.equals(watchEvent.kind()) || ENTRY_MODIFY.equals(watchEvent.kind())) {
        performNewOrChangedPath(resolve((Path) watchKey.watchable(), (Path) watchEvent.context()), watchService);
      } else if (ENTRY_DELETE.equals(watchEvent.kind())) {
        performRemovedPath(resolve((Path) watchKey.watchable(), (Path) watchEvent.context()), watchService);
      }
    }
  }

  private Path resolve(Path parent, Path path) {
    return Optional.ofNullable(parent)
        .map(newPath -> newPath.resolve(path).normalize())
        .orElse(path.normalize());
  }

  private void performNewOrChangedPath(Path path, WatchService watchService) {
    try {
      if (isRootPath(path)) {
        return;
      }
      if (Files.isDirectory(path.toAbsolutePath())) {
        if (!checksums.containsKey(path.toAbsolutePath().normalize())) {
          log.info("Watch Directory {} for changes", path);
          path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        }
      }
      log.info("Calculate the Checksum of File {}", path.toAbsolutePath());
      String checksum = createChecksum(path);
      checksums.put(path.toAbsolutePath().normalize(), checksum);
      changeListeners.stream()
          .forEach(consumer -> consumer.accept(path));
      performNewOrChangedPath(path.getParent(), watchService);
    } catch (Exception e) {
      log.error(String.format("Error occured while perforing new or changed Path %s", path.toAbsolutePath()), e);
    }
  }

  private boolean isRootPath(Path path) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path rootPath = resourceProperties.getLocation();
    try {
      return Files.isSameFile(path.toAbsolutePath().normalize(), rootPath.toAbsolutePath().normalize());
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  private void performRemovedPath(Path path, WatchService watchService) {
    checksums.remove(path.toAbsolutePath().normalize());
    deleteListeners.stream()
        .forEach(consumer -> consumer.accept(path));
    performNewOrChangedPath(path.getParent(), watchService);
  }

  @Override
  public String getChecksum(Path path) throws OwncloudResourceException {
    return checksums.get(path.toAbsolutePath().normalize());
  }

  @Override
  public void registerChangeListener(Consumer<Path> listener) {
    changeListeners.add(listener);
  }

  @Override
  public void registerDeleteListener(Consumer<Path> listener) {
    deleteListeners.add(listener);
  }

  @Override
  public void clearChangeListener() {
    changeListeners.clear();
  }

  @Override
  public void clearDeleteListener() {
    deleteListeners.clear();
  }

  @Slf4j
  private static class InitializingFileVisitor extends SimpleFileVisitor<Path> {

    private final WatchService watchService;
    private final Function<Path, String> fileDigest;
    private final BiFunction<Path, Map<Path, String>, String> directoryDigest;

    @Getter
    private final Map<Path, String> checksums = new HashMap<>();

    @Builder
    private InitializingFileVisitor(
        final WatchService watchService,
        final Function<Path, String> fileDigest,
        final BiFunction<Path, Map<Path, String>, String> directoryDigest) {
      this.watchService = watchService;
      this.fileDigest = fileDigest;
      this.directoryDigest = directoryDigest;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      log.info("Calculate the Checksum of File {}", file);
      String checksum = fileDigest.apply(file);
      checksums.put(file.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      String checksum = directoryDigest.apply(dir, checksums);
      checksums.put(dir.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

  }
}
