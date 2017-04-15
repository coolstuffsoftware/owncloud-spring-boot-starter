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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.val;
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
  private OwncloudLocalUserService userService;

  private Map<String, OwncloudLocalQuotaImpl> quotas = new HashMap<>();

  @PostConstruct
  protected void afterPropertiesSet() throws Exception {
    OwncloudLocalProperties.ResourceServiceProperties resourceProperties = properties.getResourceService();
    Validate.notNull(resourceProperties);
    Validate.notNull(resourceProperties.getLocation());

    Path baseLocation = resourceProperties.getLocation();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(baseLocation);

    calculateQuotas(baseLocation);

    log.debug("Register Usermodification Callbacks");
    userService.registerSaveUserCallback(this::notifyUserModification);
    userService.registerDeleteUserCallback(this::notifyRemovedUser);
  }

  private void calculateQuotas(Path baseLocation) throws IOException {
    quotas.clear();
    userDataService.getUsers()
        .forEach(user -> {
          String username = user.getUsername();
          OwncloudLocalQuotaImpl quota = calculateUsedSpace(username, baseLocation);
          quota.setTotal(user.getQuota());
          quotas.put(username, quota);
        });
  }

  private OwncloudLocalQuotaImpl calculateUsedSpace(String username, Path baseLocation) {
    Path userBaseLocation = baseLocation.resolve(username);

    if (Files.notExists(userBaseLocation)) {
      return OwncloudLocalQuotaImpl.builder()
          .username(username)
          .location(baseLocation)
          .build();
    }

    try {
      OwncloudLocalQuotaImpl quota = OwncloudLocalQuotaImpl.builder()
          .username(username)
          .location(userBaseLocation)
          .build();
      log.debug("Calculate the Space used by User {}", username);
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
    log.debug("User {} has been changed or created -> change the Quota to {}", userDetails.getUsername(), userDetails.getQuota());
    OwncloudLocalQuotaImpl quota = quotas.computeIfAbsent(userDetails.getUsername(), this::getOrCreateQuota);
    quota.setTotal(userDetails.getQuota());
  }

  private OwncloudLocalQuotaImpl getOrCreateQuota(String username) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    return calculateUsedSpace(username, resourceProperties.getLocation());
  }

  private void notifyRemovedUser(String username) {
    log.debug("User {} has been removed -> remove the Quota Information", username);
    quotas.remove(username);
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) {
    Path location = resolveLocation(relativeTo);
    return resourcesOf(location);
  }

  private Path resolveLocation(URI relativeTo) {
    Path location = getRootLocationOfAuthenticatedUser();
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return location;
    }
    createDirectoryIfNotExists(location);
    String relativeToPath = relativeTo.getPath();
    if (StringUtils.startsWith(relativeToPath, "/")) {
      relativeToPath = relativeToPath.substring(1);
    }
    return location.resolve(relativeToPath);
  }

  private Path getRootLocationOfAuthenticatedUser() {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path location = resourceProperties.getLocation();
    val username = getUsername();
    location = location.resolve(username);
    checkIfExistingDirectory(location);
    log.debug("Resolved Base Location of User {}: {}", username, location);
    return location;
  }

  private String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  private void checkIfExistingDirectory(Path location) {
    if (isNotExistingDirectory(location)) {
      val logMessage = String.format("Existing Path %s is not a Directory", location);
      log.error(logMessage);
      throw new OwncloudLocalResourceException(logMessage);
    }
  }

  private boolean isNotExistingDirectory(Path location) {
    return Files.exists(location) && !Files.isDirectory(location);
  }

  private void createDirectoryIfNotExists(Path location) {
    if (Files.notExists(location)) {
      try {
        log.debug("Create Directory {}", location);
        Files.createDirectories(location);
      } catch (IOException e) {
        val logMessage = String.format("Could not create Directory %s", location);
        log.error(logMessage, e);
        throw new OwncloudLocalResourceException(logMessage, e);
      }
    }
  }

  private List<OwncloudResource> resourcesOf(Path location) {
    if (Files.isDirectory(location)) {
      return getDirectoryResources(location);
    }
    return Stream.of(createOwncloudResourceOf(location))
        .peek(resource -> log.debug("Add Resource {} to the Result", resource.getHref()))
        .collect(Collectors.toList());
  }

  private List<OwncloudResource> getDirectoryResources(Path location) {
    try {
      List<OwncloudResource> owncloudResources = new ArrayList<>();
      owncloudResources.add(getActualDirectoryOf(location));
      Files.list(location)
          .map(path -> createOwncloudResourceOf(path))
          .peek(resource -> log.debug("Add Resource {} to the Result", resource.getHref()))
          .forEach(owncloudResources::add);
      getParentDirectoryOf(location)
          .ifPresent(owncloudResources::add);
      return owncloudResources;
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  private OwncloudModifyingLocalResource getActualDirectoryOf(Path location) {
    OwncloudModifyingLocalResource actualDirectory = createOwncloudResourceOf(location);
    log.debug("Add actual Directory {} to the Result", actualDirectory.getHref());
    actualDirectory.setName(".");
    return actualDirectory;
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
      val logMessage = String.format("Cannot create OwncloudResource from Path %s", path);
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  private Optional<OwncloudModifyingLocalResource> getParentDirectoryOf(Path location) {
    if (isParentDirectoryNotAppendable(location)) {
      return Optional.empty();
    }

    OwncloudModifyingLocalResource superDirectory = createOwncloudResourceOf(location.resolve("..").normalize());
    log.debug("Add parent Directory of Location {} ({}) to the Result", location, superDirectory.getHref());
    superDirectory.setName("..");
    return Optional.of(superDirectory);
  }

  private boolean isParentDirectoryNotAppendable(Path location) {
    return !properties.getResourceService().isAddRelativeDownPath() || isRootDirectory(location);
  }

  private boolean isRootDirectory(Path location) {
    if (!Files.isDirectory(location)) {
      return false;
    }
    Path rootLocation = getRootLocationOfAuthenticatedUser();
    try {
      return Files.isSameFile(location, rootLocation);
    } catch (IOException e) {
      val logMessage = String.format("Cannot determine the equality of path %s to the base Location %s", location, rootLocation);
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Override
  public Optional<OwncloudResource> find(URI path) {
    Path location = resolveLocation(path);
    if (Files.notExists(location)) {
      return Optional.empty();
    }
    log.debug("Get Information about Resource %s", path);
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
      log.debug("Create Directory {}", location.toAbsolutePath().normalize());
      Files.createDirectory(location);
      checksumService.recalculateChecksum(location);
      return createOwncloudResourceOf(location);
    } catch (IOException e) {
      val logMessage = String.format("Cannot create Directory %s", location.toAbsolutePath().normalize());
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Override
  public void delete(OwncloudResource resource) {
    Path path = resolveLocation(resource.getHref());
    checkExistence(path, resource);
    removeExistingPath(path);
  }

  private void checkExistence(Path path, OwncloudResource resource) {
    if (Files.notExists(path)) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      throw new OwncloudResourceNotFoundException(resource.getHref(), authentication.getName());
    }
  }

  private void removeExistingPath(Path path) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudLocalQuotaImpl quota = quotas.get(authentication.getName());
    removeExistingPathAndRecalculateSpaceAndChecksum(path, quota);
  }

  private void removeExistingPathAndRecalculateSpaceAndChecksum(Path path, OwncloudLocalQuotaImpl quota) {
    try {
      if (Files.isDirectory(path)) {
        log.debug("Remove Directory {} with all its Content and reduce the used Space of User {}", path.toAbsolutePath().normalize(), quota.getUsername());
        Files.walkFileTree(path, new DeleteFileVisitor(quota::reduceUsed));
      } else {
        log.debug("Remove File {} and reduce the used Space of User {}", path.toAbsolutePath().normalize(), quota.getUsername());
        quota.reduceUsed(Files.size(path));
        Files.delete(path);
      }
    } catch (IOException e) {
      val logMessage = String.format("Cannot remove Path %s", path.toAbsolutePath().normalize());
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
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
      log.debug("Return InputStream of File {}", location.toAbsolutePath().normalize());
      return Files.newInputStream(location);
    } catch (NoSuchFileException e) {
      log.warn("File {} not found", location.toAbsolutePath().normalize());
      throw new OwncloudResourceNotFoundException(resource.getHref(), getUsername());
    } catch (IOException e) {
      val logMessage = String.format("Cannot get InputStream of File %s", location.toAbsolutePath().normalize());
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) {
    return getOutputStream(resource.getHref(), resource.getMediaType());
  }

  @Override
  public OutputStream getOutputStream(URI path, MediaType mediaType) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    log.debug("Create a piped OutputStream to control the written Data (because of the Quota of User {}", authentication.getName());
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
      log.error("User {} exceeded its Quota of {} Bytes", quota.getUsername(), quota.getTotal());
      removeFile(environment);
      throw new OwncloudQuotaExceededException(environment.getUri(), environment.getUsername());
    }
  }

  private boolean isNoMoreSpaceLeft(OwncloudQuota quota, PipedOutputStreamAfterCopyEnvironment environment) {
    return quota.getFree() < environment.getContentLength();
  }

  private void removeFile(PipedOutputStreamAfterCopyEnvironment environment) {
    try {
      log.debug("Remove File {}", environment.getPath().toAbsolutePath().normalize());
      Files.delete(environment.getPath());
    } catch (IOException e) {
      final String logMessage = String.format("Error while removing File %s", environment.getPath().toAbsolutePath().normalize());
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Override
  public OwncloudQuota getQuota() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    log.debug("Return the actual Quota of User {}", authentication.getName());
    return quotas.get(authentication.getName());
  }

  @Override
  public void resetAllUsedSpace() {
    quotas.forEach(this::resetUsedSpace);
  }

  private void resetUsedSpace(String username, OwncloudLocalQuotaImpl quota) {
    log.debug("Reset the used Space of User {}", username);
    quota.setUsed(0);
  }

  @Override
  public void recalculateAllUsedSpace() {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path baseLocation = resourceProperties.getLocation();
    quotas.forEach((username, unusedQuota) -> {
      quotas.computeIfPresent(username, (unusedUsername, existingQuota) -> {
        OwncloudLocalQuotaImpl quota = calculateUsedSpace(username, baseLocation);
        quota.setTotal(existingQuota.getTotal());
        return quota;
      });
    });
  }
}
