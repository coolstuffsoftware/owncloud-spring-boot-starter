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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.net.URI;

/**
 * Service Interface to resolve a root URI.
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public interface OwncloudResolveRootUriService {

  /**
   * Get resolved Root URI.
   * <p/>
   * Would be used to the the resolved Root URI within the Test Classes
   * @param username Username to resolve te Root URI to
   * @return resolved root URI
   */
  URI getResolvedRootUri(String username);

}
