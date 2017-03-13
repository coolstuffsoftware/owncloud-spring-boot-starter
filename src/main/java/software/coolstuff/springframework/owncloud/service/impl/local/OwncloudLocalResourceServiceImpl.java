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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@Slf4j
class OwncloudLocalResourceServiceImpl implements OwncloudResourceService {

  @Autowired
  private OwncloudLocalProperties properties;

  @Autowired
  private OwncloudLocalResourceChecksumService checksumService;

  @PostConstruct
  protected void afterPropertiesSet() throws Exception {
    OwncloudLocalProperties.ResourceServiceProperties resourceProperties = properties.getResourceService();
    Validate.notNull(resourceProperties);
    Validate.notNull(resourceProperties.getLocation());

    Path baseLocation = resourceProperties.getLocation();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(baseLocation);
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) {
    Path location = resolveLocation(relativeTo);
    List<OwncloudResource> owncloudResources = new ArrayList<>();
    if (Files.isDirectory(location)) {
      addDirectoryResource(location, owncloudResources);
    } else {
      owncloudResources.add(createOwncloudResourceFrom(location));
    }
    return owncloudResources;
  }

  private void addDirectoryResource(Path location, List<OwncloudResource> owncloudResources) {
    try {
      OwncloudModifyingLocalResource actualDirectory = createOwncloudResourceFrom(location);
      actualDirectory.setName(".");
      owncloudResources.add(actualDirectory);
      owncloudResources.addAll(
          Files.list(location)
              .map(path -> createOwncloudResourceFrom(path))
              .collect(Collectors.toList()));
      appendParentDirectoryOf(location, owncloudResources);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
  }

  private void appendParentDirectoryOf(Path location, List<OwncloudResource> owncloudResources) {
    if (isParentDirectoryAppendable(location)) {
      location = location.resolve("..").normalize();
      OwncloudModifyingLocalResource superDirectory = createOwncloudResourceFrom(location);
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

  private OwncloudModifyingLocalResource createOwncloudResourceFrom(Path path) {
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
  public OwncloudResource find(URI path) {
    Path location = resolveLocation(path);
    if (Files.notExists(location)) {
      throw new OwncloudResourceNotFoundException(path, getUsername());
    }
    return createOwncloudResourceFrom(location);
  }

  @Override
  public OwncloudResource createDirectory(URI directory) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(OwncloudResource resource) {
    // TODO Auto-generated method stub
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
  public OutputStream getOutputStream(URI href, MediaType mediaType) {
    Path location = resolveLocation(href);
    try {
      if (Files.notExists(location)) {
        Files.createFile(location);
      }
      return new ContentOutputStream(location);
    } catch (IOException e) {
      log.error(String.format("Error while retrieving Content of File %s", location.toAbsolutePath().normalize().toString()), e);
      throw new OwncloudLocalResourceException(e);
    }
  }

  private class ContentOutputStream extends FileOutputStream {

    private final Path path;

    public ContentOutputStream(final Path path) throws FileNotFoundException {
      super(path.toFile());
      this.path = path;
    }

    @Override
    public void close() {
      try {
        super.close();
        checksumService.recalculateChecksum(path);
      } catch (IOException e) {
        throw new OwncloudLocalResourceException(e);
      }
    }

  }
}
