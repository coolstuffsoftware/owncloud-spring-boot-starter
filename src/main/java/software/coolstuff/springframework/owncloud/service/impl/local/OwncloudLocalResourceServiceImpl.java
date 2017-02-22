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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
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
    List<OwncloudResource> resources = new ArrayList<>();
    if (Files.isDirectory(location)) {
      try {
        OwncloudModifyingLocalResource actualDirectory = createResourceFrom(location);
        if (Files.isDirectory(location)) {
          resources.addAll(
              Files.list(location)
                  .map(path -> createResourceFrom(path))
                  .collect(Collectors.toList()));
          if (isNotRootDirectory(location)) {
            actualDirectory.setName(".");
            if (properties.getResourceService().isAddRelativeDownPath()) {
              location = location.resolve("..");
              resources.add(createResourceFrom(location));
            }
          }
        }
        resources.add(actualDirectory);
      } catch (IOException e) {
        throw new OwncloudResourceException(e) {
          private static final long serialVersionUID = -4406347844686894254L;
        };
      }
    } else {
      resources.add(createResourceFrom(location));
    }
    return resources;
  }

  private Path resolveLocation(URI relativeTo) {
    Path location = getRootLocationOfAuthenticatedUser();
    if (Files.notExists(location)) {
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

  private Path getRootLocationOfAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path location = resourceProperties.getLocation();
    location = location.resolve(authentication.getName());
    return location;
  }

  private OwncloudModifyingLocalResource createResourceFrom(Path path) {
    try {
      Path rootPath = getRootLocationOfAuthenticatedUser();
      Path relativePath = rootPath.toAbsolutePath().relativize(path.toAbsolutePath());
      URI href = URI.create(
          UriComponentsBuilder
              .fromPath("/")
              .path(relativePath.toString())
              .toUriString());
      String name = path.getFileName().toString();
      if (Files.isSameFile(rootPath, path)) {
        name = "/";
      }
      MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
      if (Files.isDirectory(path)) {
        mediaType = OwncloudUtils.getDirectoryMediaType();
      } else {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentType = fileNameMap.getContentTypeFor(path.getFileName().toString());
        if (StringUtils.isNotBlank(contentType)) {
          mediaType = MediaType.valueOf(contentType);
        }
      }
      Date lastModifiedAt = Date.from(Files.getLastModifiedTime(path).toInstant());
      String checksum = checksumService.getChecksum(path);
      OwncloudModifyingLocalResource resource = OwncloudLocalResourceImpl.builder()
          .href(href)
          .name(name)
          .eTag(checksum)
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
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 7484650505520708669L;
      };
    }
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
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 6416480160654068104L;
      };
    }
  }

  @Override
  public OwncloudResource find(URI path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudFileResource createFile(URI file) {
    // TODO Auto-generated method stub
    return null;
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) {
    // TODO Auto-generated method stub
    return null;
  }
}
