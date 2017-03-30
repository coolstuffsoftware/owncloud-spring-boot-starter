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
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

/**
 * Access Files and Directories on the Owncloud.
 * <p/>
 * Only Files and Directories of the User which has been authenticated
 * by the OwncloudAuthenticationProvider can be accessed.
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public interface OwncloudResourceService {

  /**
   * Get all Files and Directories of the Root Directory of the User
   * @return List of Resources relative to the User Root Directory
   */
  default List<OwncloudResource> listRoot() {
    return list(null);
  }

  /**
   * Get all Files and Directories relative to the given Path of the URI
   * @param relativeTo Path relative to the User Root Directory
   * @return List of Resources relative to the given Path
   */
  List<OwncloudResource> list(URI relativeTo);

  /**
   * Find a specific {@link OwncloudResource} specified by the Path of the URI
   * @param path Path to a specific Resource (File or Directory)
   * @return found OwncloudResource or {@link Optional#empty()} when the Resource has not been found
   */
  Optional<OwncloudResource> find(URI path);

  /**
   * Create a new Directory
   * @param directory Path of the new Directory relative to the User Root Directory
   * @return Resource Information of the created Directory
   */
  OwncloudResource createDirectory(URI directory);

  /**
   * Delete a Resource.
   * <p/>
   * <strong>Caution:</strong> When the Resource represents a Directory
   * then all Contents of this Directory will be removed recursively.
   * @param resource Directory or File Resource returned by {@link #listRoot()}, {@link #list(URI)} or {@link #find(URI)}
   * @see #listRoot()
   * @see #list(URI)
   * @see #find(URI)
   */
  void delete(OwncloudResource resource);

  /**
   * Get the Content of a File Resource
   * @param resource File Resource returned by {@link #listRoot()}, {@link #list(URI)} or {@link #find(URI)}
   * @return Content of the File Resource
   * @see #listRoot()
   * @see #list(URI)
   * @see #find(URI)
   * @see OwncloudUtils#toOwncloudFileResource(OwncloudResource)
   */
  InputStream getInputStream(OwncloudFileResource resource);

  /**
   * Overwrite the Content of an existing File Resource
   * @param resource existing File Resource returned by {@link #listRoot()}, {@link #list(URI)} or {@link #find(URI)}
   * @return OutputStream to write a new Content
   * @see #listRoot()
   * @see #list(URI)
   * @see #find(URI)
   * @see OwncloudUtils#toOwncloudFileResource(OwncloudResource)
   */
  OutputStream getOutputStream(OwncloudFileResource resource);

  /**
   * Create a new File Resource or overwrite the Content of an existing File Resource
   * @param path Path of the new or existing File Resource
   * @param mediaType Media Type of the new File Resource
   * @return OutputStream to write a new Content
   */
  OutputStream getOutputStream(URI path, MediaType mediaType);

  /**
   * Get the actual Quota Information of the authenticated User
   * @return Quota of the authenticated User
   */
  OwncloudQuota getQuota();

}
