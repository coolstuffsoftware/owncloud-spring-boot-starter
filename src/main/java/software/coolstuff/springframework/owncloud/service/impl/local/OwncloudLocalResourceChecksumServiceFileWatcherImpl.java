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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceWatchServiceException;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.FileWatcherThreadProperties;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudLocalResourceChecksumServiceFileWatcherImpl extends AbstractOwncloudLocalResourceChecksumServiceImpl {

  private Thread fileWatcherThread;

  private final Map<Path, String> checksums = new ConcurrentHashMap<>();

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    ResourceServiceProperties resourceProperties = getResourceProperties();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(resourceProperties.getLocation());

    FileWatcherThreadProperties fileWatcherThreadProperties = resourceProperties.getFileWatcherThread();
    fileWatcherThread = new Thread(this::fileWatcher);
    fileWatcherThread.setName(fileWatcherThreadProperties.getName());
    fileWatcherThread.setPriority(fileWatcherThreadProperties.getPriority());
    fileWatcherThread.start();
  }

  private void fileWatcher() {
    log.info("Starting FileWatcher Thread {}", Thread.currentThread().getName());
    ResourceServiceProperties resourceProperties = getResourceProperties();
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
      ResourceServiceProperties resourceProperties = getResourceProperties();
      val fileVisitor = InitializingFileVisitor.builder()
          .checksums(checksums)
          .watchService(watchService)
          .fileDigest(this::createFileChecksum)
          .directoryDigest(this::createDirectoryChecksum)
          .build();
      log.debug("Get the Checksums of all Files and Directories under Path {}", resourceProperties.getLocation());
      Files.walkFileTree(resourceProperties.getLocation(), fileVisitor);
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
      synchronized (this) {
        getChangeListeners().stream()
            .forEach(consumer -> consumer.accept(path));
      }
      performNewOrChangedPath(path.getParent(), watchService);
    } catch (Exception e) {
      log.error(String.format("Error occured while perforing new or changed Path %s", path.toAbsolutePath()), e);
    }
  }

  private String createChecksum(Path path) {
    if (Files.isDirectory(path)) {
      return createDirectoryChecksum(path, checksums);
    }
    return createFileChecksum(path);
  }

  private boolean isRootPath(Path path) {
    ResourceServiceProperties resourceProperties = getResourceProperties();
    Path rootPath = resourceProperties.getLocation();
    try {
      return Files.isSameFile(path.toAbsolutePath().normalize(), rootPath.toAbsolutePath().normalize());
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  private void performRemovedPath(Path path, WatchService watchService) {
    checksums.remove(path.toAbsolutePath().normalize());
    synchronized (this) {
      getDeleteListeners().stream()
          .forEach(consumer -> consumer.accept(path));
    }
    performNewOrChangedPath(path.getParent(), watchService);
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
  public ChecksumServiceStrategy getStrategy() {
    return ChecksumServiceStrategy.FILE_WATCHER;
  }

}
