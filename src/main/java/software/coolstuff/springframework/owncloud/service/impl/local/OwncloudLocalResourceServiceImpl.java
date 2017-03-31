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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudNoDirectoryResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudQuotaExceededException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@Slf4j
class OwncloudLocalResourceServiceImpl implements OwncloudLocalResourceService {

  @Autowired
  private OwncloudLocalProperties properties;

  @Autowired
  private OwncloudLocalResourceChecksumService checksumService;

  @Autowired
  private OwncloudLocalUserDataService userDataService;

  @Autowired
  private OwncloudLocalUserModificationService userModificationService;

  private Map<String, OwncloudLocalQuota> quotas = new HashMap<>();

  @PostConstruct
  protected void afterPropertiesSet() throws Exception {
    OwncloudLocalProperties.ResourceServiceProperties resourceProperties = properties.getResourceService();
    Validate.notNull(resourceProperties);
    Validate.notNull(resourceProperties.getLocation());

    Path baseLocation = resourceProperties.getLocation();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(baseLocation);

    calculateQuotas(baseLocation);
    userModificationService.registerSaveUserCallback(this::notifyUserModification);
    userModificationService.registerDeleteUserCallback(this::notifyRemovedUser);
  }

  private void calculateQuotas(Path baseLocation) throws IOException {
    quotas.clear();
    userDataService.getUsers()
        .forEach(user -> {
          String username = user.getUsername();
          OwncloudLocalQuota quota = getOrCreateQuota(username, baseLocation);
          quota.setTotal(user.getQuota());
          quotas.put(username, quota);
        });
  }

  private OwncloudLocalQuota getOrCreateQuota(String username, Path baseLocation) {
    Path userBaseLocation = baseLocation.resolve(username);

    if (Files.notExists(userBaseLocation)) {
      return OwncloudLocalQuota.builder()
          .username(username)
          .location(baseLocation)
          .build();
    }

    try {
      OwncloudLocalQuota quota = OwncloudLocalQuota.builder()
          .username(username)
          .location(userBaseLocation)
          .build();
      Files.walkFileTree(userBaseLocation, new UsedSpaceFileVisitor(quota::increaseUsed));
      return quota;
    } catch (IOException e) {
      String logMessage = "IOException while calculating the used Space of Location " + userBaseLocation.toAbsolutePath().normalize().toString();
      log.error(logMessage);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @RequiredArgsConstructor
  private static class UsedSpaceFileVisitor extends SimpleFileVisitor<Path> {
    private final Consumer<Long> usedSpaceIncreaseConsumer;

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      usedSpaceIncreaseConsumer.accept(Files.size(file));
      return FileVisitResult.CONTINUE;
    }
  }

  private void notifyUserModification(OwncloudUserDetails userDetails) {
    OwncloudLocalQuota quota = quotas.computeIfAbsent(userDetails.getUsername(), this::getOrCreateQuota);
    quota.setTotal(userDetails.getQuota());
  }

  private OwncloudLocalQuota getOrCreateQuota(String username) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    return getOrCreateQuota(username, resourceProperties.getLocation());
  }

  private void notifyRemovedUser(String username) {
    quotas.remove(username);
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) {
    Path location = resolveLocation(relativeTo);
    List<OwncloudResource> owncloudResources = new ArrayList<>();
    if (Files.isDirectory(location)) {
      addDirectoryResource(location, owncloudResources);
    } else {
      owncloudResources.add(createOwncloudResourceOf(location));
    }
    return owncloudResources;
  }

  private void addDirectoryResource(Path location, List<OwncloudResource> owncloudResources) {
    try {
      OwncloudModifyingLocalResource actualDirectory = createOwncloudResourceOf(location);
      actualDirectory.setName(".");
      owncloudResources.add(actualDirectory);
      owncloudResources.addAll(
          Files.list(location)
              .map(path -> createOwncloudResourceOf(path))
              .collect(Collectors.toList()));
      appendParentDirectoryOf(location, owncloudResources);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  private void appendParentDirectoryOf(Path location, List<OwncloudResource> owncloudResources) {
    if (isParentDirectoryAppendable(location)) {
      location = location.resolve("..").normalize();
      OwncloudModifyingLocalResource superDirectory = createOwncloudResourceOf(location);
      superDirectory.setName("..");
      owncloudResources.add(superDirectory);
    }
  }

  private boolean isParentDirectoryAppendable(Path location) {
    return properties.getResourceService().isAddRelativeDownPath() && isNotRootDirectory(location);
  }

  private Path resolveLocation(URI relativeTo) {
    Path location = getRootLocationOfAuthenticatedUser();
    if (Files.notExists(location)) {
      try {
        Files.createDirectories(location);
      } catch (IOException e) {
        throw new OwncloudLocalResourceException(e);
      }
    }
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return location;
    }
    String relativeToPath = relativeTo.getPath();
    if (StringUtils.startsWith(relativeToPath, "/")) {
      relativeToPath = relativeToPath.substring(1);
    }
    return location.resolve(relativeToPath);
  }

  private Path getRootLocationOfAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path location = resourceProperties.getLocation();
    location = location.resolve(authentication.getName());
    return location;
  }

  private OwncloudModifyingLocalResource createOwncloudResourceOf(Path path) {
    Path rootPath = getRootLocationOfAuthenticatedUser();
    Path relativePath = rootPath.toAbsolutePath().relativize(path.toAbsolutePath());
    URI href = URI.create(
        UriComponentsBuilder
            .fromPath("/")
            .path(relativePath.toString())
            .toUriString());
    String name = path.getFileName().toString();
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (Files.isDirectory(path)) {
      href = URI.create(
          UriComponentsBuilder.fromUri(href)
              .path("/")
              .toUriString());
      mediaType = OwncloudUtils.getDirectoryMediaType();
    } else {
      FileNameMap fileNameMap = URLConnection.getFileNameMap();
      String contentType = fileNameMap.getContentTypeFor(path.getFileName().toString());
      if (StringUtils.isNotBlank(contentType)) {
        mediaType = MediaType.valueOf(contentType);
      }
    }
    try {
      Date lastModifiedAt = Date.from(Files.getLastModifiedTime(path).toInstant());
      Optional<String> checksum = checksumService.getChecksum(path);
      if (Files.isSameFile(rootPath, path)) {
        name = "/";
        checksum = Optional.empty();
      }
      OwncloudModifyingLocalResource resource = OwncloudLocalResourceImpl.builder()
          .href(href)
          .name(name)
          .eTag(checksum.orElse(null))
          .mediaType(mediaType)
          .lastModifiedAt(lastModifiedAt)
          .build();
      if (Files.isDirectory(path)) {
        return resource;
      }

      return OwncloudLocalFileResourceImpl.fileBuilder()
          .owncloudResource(resource)
          .contentLength(Files.size(path))
          .build();
    } catch (NoSuchFileException e) {
      throw new OwncloudResourceNotFoundException(href, getUsername());
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  private String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  private boolean isNotRootDirectory(Path location) {
    return !isRootDirectory(location);
  }

  private boolean isRootDirectory(Path location) {
    if (!Files.isDirectory(location)) {
      return false;
    }
    Path rootLocation = getRootLocationOfAuthenticatedUser();
    try {
      return Files.isSameFile(location, rootLocation);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  @Override
  public Optional<OwncloudResource> find(URI path) {
    Path location = resolveLocation(path);
    if (Files.notExists(location)) {
      return Optional.empty();
    }
    return Optional.ofNullable(createOwncloudResourceOf(location));
  }

  @Override
  public OwncloudResource createDirectory(URI directory) {
    Path location = resolveLocation(directory);
    if (Files.exists(location)) {
      if (Files.isDirectory(location)) {
        return createOwncloudResourceOf(location);
      }
      throw new OwncloudNoDirectoryResourceException(directory);
    }
    try {
      Files.createDirectory(location);
      checksumService.recalculateChecksum(location);
      return createOwncloudResourceOf(location);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  @Override
  public void delete(OwncloudResource resource) {
    Path path = resolveLocation(resource.getHref());
    checkPathExists(path, resource);
    removeExistingPath(path);
  }

  private void checkPathExists(Path path, OwncloudResource resource) {
    if (Files.notExists(path)) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      throw new OwncloudResourceNotFoundException(resource.getHref(), authentication.getName());
    }
  }

  private void removeExistingPath(Path path) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudLocalQuota quota = quotas.get(authentication.getName());
    removeExistingPathAndRecalculateSpaceAndChecksum(path, quota);
  }

  private void removeExistingPathAndRecalculateSpaceAndChecksum(Path path, OwncloudLocalQuota quota) {
    try {
      if (Files.isDirectory(path)) {
        Files.walkFileTree(path, new DeleteFileVisitor(quota::reduceUsed));
      } else {
        quota.reduceUsed(Files.size(path));
        Files.delete(path);
      }
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    } finally {
      checksumService.recalculateChecksum(path);
    }
  }

  @RequiredArgsConstructor
  private static class DeleteFileVisitor extends SimpleFileVisitor<Path> {
    private final Consumer<Long> usedSpaceReductionConsumer;

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      usedSpaceReductionConsumer.accept(Files.size(file));
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  }

  @Override
  public InputStream getInputStream(OwncloudFileResource resource) {
    Path location = resolveLocation(resource.getHref());
    try {
      return Files.newInputStream(location);
    } catch (NoSuchFileException e) {
      throw new OwncloudResourceNotFoundException(resource.getHref(), getUsername());
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) {
    return getOutputStream(resource.getHref(), resource.getMediaType());
  }

  @Override
  public OutputStream getOutputStream(URI path, MediaType mediaType) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PipedOutputStreamLocalSynchronizer pipedStreamSynchronizer = PipedOutputStreamLocalSynchronizer.builder()
        .authentication(authentication)
        .afterCopyCallback(this::afterCopy)
        .owncloudLocalProperties(properties)
        .uri(path)
        .uriResolver(this::resolveLocation)
        .build();
    return pipedStreamSynchronizer.getOutputStream();
  }

  private void afterCopy(PipedOutputStreamAfterCopyEnvironment environment) {
    Optional
        .ofNullable(quotas.get(environment.getUsername()))
        .ifPresent(quota -> {
          checkSpace(quota, environment);
          quota.increaseUsed(environment.getContentLength());
        });
    if (Files.exists(environment.getPath())) {
      checksumService.recalculateChecksum(environment.getPath());
    }
  }

  private void checkSpace(OwncloudQuota quota, PipedOutputStreamAfterCopyEnvironment environment) {
    if (isNoMoreSpaceLeft(quota, environment)) {
      removeFile(environment);
      throw new OwncloudQuotaExceededException(environment.getUri(), environment.getUsername());
    }
  }

  private boolean isNoMoreSpaceLeft(OwncloudQuota quota, PipedOutputStreamAfterCopyEnvironment environment) {
    return quota.getFree() < environment.getContentLength();
  }

  private void removeFile(PipedOutputStreamAfterCopyEnvironment environment) {
    try {
      Files.delete(environment.getPath());
    } catch (IOException e) {
      final String logMessage = String.format("Error while removing File %s", environment.getPath().toAbsolutePath().normalize());
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Override
  public OwncloudQuota getQuota() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return quotas.get(authentication.getName());
  }

  @Override
  public void resetAllUsedSpace() {
    quotas.forEach(this::resetUsedSpace);
  }

  private void resetUsedSpace(String username, OwncloudLocalQuota quota) {
    quota.setUsed(0);
  }

  @Override
  public void recalculateAllUsedSpace() {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path baseLocation = resourceProperties.getLocation();
    quotas.forEach((username, unusedQuota) -> {
      quotas.computeIfPresent(username, (unusedUsername, existingQuota) -> {
        OwncloudLocalQuota quota = getOrCreateQuota(username, baseLocation);
        quota.setTotal(existingQuota.getTotal());
        return quota;
      });
    });
  }
}
