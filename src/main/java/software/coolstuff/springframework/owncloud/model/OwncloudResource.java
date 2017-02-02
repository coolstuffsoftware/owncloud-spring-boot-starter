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
package software.coolstuff.springframework.owncloud.model;

import java.net.URI;
import java.util.Date;

import org.springframework.http.MediaType;

/**
 * @author mufasa1976
 *
 */
public interface OwncloudResource {

  String UNIX_DIRECTORY_VALUE = "httpd/unix-directory";
  MediaType UNIX_DIRECTORY = new MediaType(UNIX_DIRECTORY_VALUE);

  URI getHref();

  String getName();

  Date getLastModifiedAt();

  MediaType getMediaType();

  String getETag();

}
