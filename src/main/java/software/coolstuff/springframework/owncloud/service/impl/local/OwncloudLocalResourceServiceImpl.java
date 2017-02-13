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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.Builder;
import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceWatchServiceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.FileWatchPollProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.MessageDigestAlgorithm;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudLocalResourceServiceImpl implements OwncloudResourceService {

  @Autowired
  private OwncloudLocalProperties properties;

  private MessageDigest messageDigest;

  private Thread fileWatcherThread;
  private boolean stopFileWatcherThread = false;

  private final Map<Path, Map<Path, String>> checksums = new HashMap<>();

  @PostConstruct
  protected void afterPropertiesSet() throws Exception {
    OwncloudLocalProperties.ResourceServiceProperties resourceProperties = properties.getResourceService();
    Validate.notNull(resourceProperties);
    Validate.notNull(resourceProperties.getLocation());

    Path baseLocation = resourceProperties.getLocation();
    checkPrivileges(baseLocation);

    MessageDigestAlgorithm messageDigestAlgorithm = resourceProperties.getMessageDigestAlgorithm();
    Validate.notNull(messageDigestAlgorithm);
    messageDigest = messageDigestAlgorithm.getMessageDigest();

    fileWatcherThread = new Thread(this::run);
    fileWatcherThread.setName(resourceProperties.getFileWatcherThread().getName());
    fileWatcherThread.setPriority(resourceProperties.getFileWatcherThread().getPriority());
    fileWatcherThread.start();
  }

  @PreDestroy
  public void destroy() throws Exception {
    stopFileWatcherThread = true;
    if (fileWatcherThread != null && fileWatcherThread.isAlive()) {
      fileWatcherThread.join();
    }
  }

  protected void checkPrivileges(Path baseLocation) {
    Validate.isTrue(Files.exists(baseLocation));
    Validate.isTrue(Files.isDirectory(baseLocation));
    Validate.isTrue(Files.isReadable(baseLocation)); // List Files within Directory
    Validate.isTrue(Files.isExecutable(baseLocation)); // can change into Directory
    Validate.isTrue(Files.isWritable(baseLocation)); // can create or delete Files within Directory
  }

  private void run() {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    try (WatchService watchService = resourceProperties.getLocation().getFileSystem().newWatchService()) {
      registerExistingDirectories(watchService);
      watchDirectoryChanges(watchService);
    } catch (IOException e) {
      throw new OwncloudLocalResourceWatchServiceException(e);
    }
  }

  private void registerExistingDirectories(WatchService watchService) {
    try {
      ResourceServiceProperties resourceProperties = properties.getResourceService();
      checksums.clear();
      val fileVisitor = InitializingFileVisitor.builder()
          .watchService(watchService)
          .fileDigest(this::createFileChecksum)
          .build();
      log.debug("Get the Checksums of all Files and Directories under Path {}", resourceProperties.getLocation());
      Files.walkFileTree(resourceProperties.getLocation(), fileVisitor);
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

  private void watchDirectoryChanges(WatchService watchService) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    FileWatchPollProperties pollingProperties = resourceProperties.getPolling();
    while (!stopFileWatcherThread) {
      try {
        WatchKey watchKey = watchService.poll(pollingProperties.getTimeout(), pollingProperties.getTimeUnit());
        if (watchKey != null && watchKey.isValid()) {
          performFileChange(watchKey);
        }
      } catch (InterruptedException e) {
        stopFileWatcherThread = true;
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

  @Override
  public List<OwncloudResource> list(URI relativeTo) throws OwncloudResourceException {
    Path location = resolveLocation(relativeTo);
    List<OwncloudResource> resources = new ArrayList<>();
    if (Files.isDirectory(location)) {
      try {
        resources.addAll(
            Files.list(location)
                .map(path -> createResourceFrom(path))
                .collect(Collectors.toList()));
      } catch (IOException e) {
        throw new OwncloudResourceException() {
          private static final long serialVersionUID = -4406347844686894254L;
        };
      }
    } else {
      resources.add(createResourceFrom(location));
    }
    return resources;
  }

  private Path resolveLocation(URI relativeTo) throws OwncloudResourceException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ResourceServiceProperties webdavProperties = properties.getResourceService();
    Path location = webdavProperties.getLocation();
    location = location.resolve(authentication.getName());
    if (!Files.notExists(location)) {
      try {
        Files.createDirectories(location);
      } catch (IOException e) {
        throw new OwncloudResourceException(e) {
          private static final long serialVersionUID = 6574659306271631711L;
        };
      }
    }
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return location;
    }
    return location.resolve(relativeTo.getPath());
  }

  private OwncloudResource createResourceFrom(Path path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource find(URI path) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudFileResource createFile(URI file) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(URI directory) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(OwncloudResource resource) throws OwncloudResourceException {
    // TODO Auto-generated method stub
  }

  @Override
  public InputStream getInputStream(OwncloudFileResource resource) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
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
