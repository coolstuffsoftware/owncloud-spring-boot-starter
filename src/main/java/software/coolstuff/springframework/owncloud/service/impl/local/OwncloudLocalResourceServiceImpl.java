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
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.val;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;
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
    Validate.notEmpty(webdavProperties.getLocation());

    val location = new File(webdavProperties.getLocation());
    Validate.isTrue(location.exists());
    Validate.isTrue(location.isDirectory());
    Validate.isTrue(location.canRead()); // List Files within Directory
    Validate.isTrue(location.canExecute()); // can change into Directory
    Validate.isTrue(location.canWrite()); // can create or delete Files within Directory
  }

  @Override
  public List<OwncloudResource> list() throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<OwncloudResource> list(OwncloudResource relativeTo) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudModifiableResource createFile(String fileName) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudModifiableResource createFile(String fileName, OwncloudResource relativeTo) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(String directoryName) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(String directoryName, OwncloudResource relativeTo) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(OwncloudResource owncloudResource) throws OwncloudResourceException {
    // TODO Auto-generated method stub

  }

  @Override
  public void move(OwncloudResource source, OwncloudResource destination) throws OwncloudResourceException {
    // TODO Auto-generated method stub

  }

  @Override
  public void copy(OwncloudResource source, OwncloudResource destination) throws OwncloudResourceException {
    // TODO Auto-generated method stub

  }

}
