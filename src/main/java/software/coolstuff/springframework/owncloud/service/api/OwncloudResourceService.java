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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;

import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;

/**
 * @author mufasa1976
 *
 */
public interface OwncloudResourceService {

  default List<OwncloudResource> listRoot() {
    return list(null);
  }

  List<OwncloudResource> list(URI relativeTo);

  Optional<OwncloudResource> find(URI path);

  OwncloudResource createDirectory(URI directory);

  void delete(OwncloudResource resource);

  InputStream getInputStream(OwncloudFileResource resource);

  OutputStream getOutputStream(OwncloudFileResource resource);

  OutputStream getOutputStream(URI href, MediaType mediaType);

}
