/*
   Copyright (C) 2016 by the original Authors.

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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import lombok.Builder;
import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceWatchServiceException;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudLocalResourceServiceFileWatcherThread implements Runnable {

  private final Path rootPath;
  private final MessageDigest messageDigest;
  private final String threadName;

  private long pollTimeout = 5;
  private TimeUnit pollTimeoutUnit = TimeUnit.SECONDS;
  private List<OwncloudLocalResourceFileWatcherListener> listeners = new ArrayList<>();

  private final Map<Path, Map<Path, String>> checksums = new HashMap<>();

  private Thread thread;
  private boolean stopWatchService = false;

  @Builder
  public OwncloudLocalResourceServiceFileWatcherThread(
      final Path rootPath,
      final MessageDigest messageDigest,
      final String threadName) {
    Validate.notNull(rootPath);
    Validate.notNull(messageDigest);
    Validate.notBlank(threadName);
    this.rootPath = rootPath.toAbsolutePath();
    this.messageDigest = messageDigest;
    this.threadName = threadName;
  }

  static class OwncloudLocalResourceServiceFileWatcherThreadBuilder {
    private String threadName = OwncloudLocalResourceServiceFileWatcherThread.class.getName();
  }

  public void setPollTimeout(long timeout, TimeUnit timeUnit) {
    this.pollTimeout = timeout;
    this.pollTimeoutUnit = timeUnit;
  }

  public void register(OwncloudLocalResourceFileWatcherListener listener) {
    listeners.add(listener);
  }

  public void deregister(OwncloudLocalResourceFileWatcherListener listener) {
    listeners.remove(listener);
  }

  public synchronized void stopWatchService() throws InterruptedException {
    stopWatchService = true;
    if (thread != null && thread.isAlive()) {
      thread.join();
    }
  }

  public String getChecksum(Path path) {
    Path resolvedPath = rootPath.resolve(path).toAbsolutePath().normalize();
    if (Files.isRegularFile(resolvedPath)) {
      return Optional.ofNullable(checksums.get(resolvedPath.getParent().toAbsolutePath()))
          .map(map -> map.get(resolvedPath.toAbsolutePath()))
          .orElse(null);
    }
    return Optional.ofNullable(checksums.get(resolvedPath))
        .map(map -> createDirectoryChecksum(map.values()))
        .orElse(null);
  }

  private String createDirectoryChecksum(Collection<String> fileChecksums) {
    synchronized (messageDigest) {
      messageDigest.reset();
      for (String fileChecksum : fileChecksums) {
        messageDigest.update(fileChecksum.getBytes());
      }
      return Hex.encodeHexString(messageDigest.digest());
    }
  }

  @Override
  public void run() {
    thread = Thread.currentThread();
    thread.setName(threadName);
    WatchService watchService = createWatchService();
    registerExistingDirectories(watchService);
    notifyListener(OwncloudLocalResourceFileWatcherListener.WatchEvent.INITIALIZED);
    watchDirectoryChanges(watchService);
    notifyListener(OwncloudLocalResourceFileWatcherListener.WatchEvent.STOPPED);
  }

  private WatchService createWatchService() {
    try {
      return rootPath.getFileSystem().newWatchService();
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  private void registerExistingDirectories(WatchService watchService) {
    try {
      checksums.clear();
      val fileVisitor = InitializingFileVisitor.builder()
          .watchService(watchService)
          .fileDigest(this::createFileChecksum)
          .build();
      log.debug("Get the Checksums of all Files and Directories under Path {}", rootPath);
      Files.walkFileTree(rootPath, fileVisitor);
      checksums.putAll(fileVisitor.getChecksums());
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

  private void notifyListener(OwncloudLocalResourceFileWatcherListener.WatchEvent event) {
    for (OwncloudLocalResourceFileWatcherListener listener : listeners) {
      if (isListeningForEvent(listener, event)) {
        listener.notifyListener();
      }
    }
  }

  private boolean isListeningForEvent(OwncloudLocalResourceFileWatcherListener listener, OwncloudLocalResourceFileWatcherListener.WatchEvent triggeredEvent) {
    for (OwncloudLocalResourceFileWatcherListener.WatchEvent watchEvent : listener.getWatchEvents()) {
      if (triggeredEvent.equals(watchEvent)) {
        return true;
      }
    }
    return false;
  }

  private void watchDirectoryChanges(WatchService watchService) {
    while (!stopWatchService) {
      try {
        WatchKey watchKey = watchService.poll(pollTimeout, pollTimeoutUnit);
        if (watchKey != null && watchKey.isValid()) {
          performFileChange(watchKey);
        }
      } catch (InterruptedException e) {
        stopWatchService = true;
        return;
      }
    }
  }

  private void performFileChange(WatchKey watchKey) {
    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
      if (ENTRY_CREATE.equals(watchEvent.kind()) || ENTRY_MODIFY.equals(watchEvent.kind())) {
        performNewOrChangedFile((Path) watchEvent.context());
      } else if (ENTRY_DELETE.equals(watchEvent.kind())) {
        performRemovedFile((Path) watchEvent.context());
      }
    }
  }

  private void performNewOrChangedFile(Path path) {
    if (Files.isDirectory(path)) {
      if (!checksums.containsKey(path.toAbsolutePath().normalize())) {
        checksums.put(path.toAbsolutePath().normalize(), new HashMap<>());
      }
      return;
    }
    log.info("Calculate the Checksum of File {}", path);
    String checksum = createFileChecksum(path);
    checksums.get(path.getParent().toAbsolutePath().normalize()).put(path.toAbsolutePath().normalize(), checksum);
  }

  private void performRemovedFile(Path path) {
    if (Files.isDirectory(path)) {
      checksums.remove(path.toAbsolutePath().normalize());
    } else {
      Optional.ofNullable(checksums.get(path.getParent().toAbsolutePath().normalize()))
          .ifPresent(map -> map.remove(path.toAbsolutePath().normalize()));
    }
  }

  @Slf4j
  private static class InitializingFileVisitor extends SimpleFileVisitor<Path> {

    private final WatchService watchService;
    private final Function<Path, String> fileDigest;

    @Getter
    private final Map<Path, Map<Path, String>> checksums = new HashMap<>();

    @Builder
    private InitializingFileVisitor(
        final WatchService watchService,
        final Function<Path, String> fileDigest) {
      this.watchService = watchService;
      this.fileDigest = fileDigest;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      checksums.put(dir.toAbsolutePath().normalize(), new HashMap<>());
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      log.info("Calculate the Checksum of File {}", file);
      String checksum = fileDigest.apply(file);
      checksums.get(file.getParent().toAbsolutePath().normalize()).put(file.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

  }
}
