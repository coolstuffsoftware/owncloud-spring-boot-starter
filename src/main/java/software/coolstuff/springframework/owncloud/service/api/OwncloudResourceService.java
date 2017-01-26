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
package software.coolstuff.springframework.owncloud.service.api;

import java.util.List;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;

/**
 * @author mufasa1976
 *
 */
public interface OwncloudResourceService {

  List<OwncloudResource> list() throws OwncloudResourceException;

  List<OwncloudResource> list(OwncloudResource relativeTo) throws OwncloudResourceException;

  OwncloudModifiableResource createFile(String fileName) throws OwncloudResourceException;

  OwncloudModifiableResource createFile(String fileName, OwncloudResource relativeTo) throws OwncloudResourceException;

  OwncloudResource createDirectory(String directoryName) throws OwncloudResourceException;

  OwncloudResource createDirectory(String directoryName, OwncloudResource relativeTo) throws OwncloudResourceException;

  void delete(OwncloudResource owncloudResource) throws OwncloudResourceException;

  void move(OwncloudResource source, OwncloudResource destination) throws OwncloudResourceException;

  void copy(OwncloudResource source, OwncloudResource destination) throws OwncloudResourceException;

}
