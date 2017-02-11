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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

/**
 * @author mufasa1976
 */
class OwncloudLocalResourceServiceImpl implements OwncloudResourceService {

  @Autowired
  private OwncloudLocalProperties properties;

  @PostConstruct
  protected void afterPropertiesSet() throws Exception {
    OwncloudLocalProperties.ResourceServiceProperties webdavProperties = properties.getResourceService();
    Validate.notNull(webdavProperties);
    Validate.notNull(webdavProperties.getLocation());

    File location = webdavProperties.getLocation().toFile();
    Validate.isTrue(location.exists());
    Validate.isTrue(location.isDirectory());
    Validate.isTrue(location.canRead()); // List Files within Directory
    Validate.isTrue(location.canExecute()); // can change into Directory
    Validate.isTrue(location.canWrite()); // can create or delete Files within Directory
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) throws OwncloudResourceException {
    File resolvedLocation = resolveLocation(relativeTo);
    List<OwncloudResource> resources = new ArrayList<>();
    if (resolvedLocation.isDirectory()) {
      for (File fileInDirectory : resolvedLocation.listFiles()) {
        resources.add(createResourceFrom(fileInDirectory));
      }
    } else {
      resources.add(createResourceFrom(resolvedLocation));
    }
    return resources;
  }

  private File resolveLocation(URI relativeTo) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Path location = properties.getResourceService().getLocation();
    location = location.resolve(authentication.getName());
    File userDirectory = location.toFile();
    if (!userDirectory.exists()) {
      userDirectory.mkdirs();
    }
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return userDirectory;
    }
    return location.resolve(relativeTo.getPath()).toFile();
  }

  private OwncloudResource createResourceFrom(File resolvedLocation) {
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

}
