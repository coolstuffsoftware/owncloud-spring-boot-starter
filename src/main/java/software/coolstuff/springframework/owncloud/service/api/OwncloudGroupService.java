/*
   Copyright (C) 2016 by the original Authors.

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

import org.springframework.security.access.AccessDeniedException;

import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;

/**
 * Modify Information of Groups on the Owncloud Server.
 * <p/>
 * This Service is only usable when the User has been authenticated
 * by the OwncloudAuthenticationProvider.
 *
 * @author mufasa1976
 * @since 1.0.0
 */
public interface OwncloudGroupService {

  /**
   * Create a new Group.
   *
   * @param groupname
   *          Name of the new Group
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties#isEnableModifications()</code> or not an Administrator
   * @throws OwncloudGroupAlreadyExistsException
   *           Group already exists
   */
  void create(String groupname);

  /**
   * Remove an existing Group.
   *
   * @param groupname
   *          Name of the Group to be removed
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties#isEnableModifications()</code> or not an Administrator
   * @throws OwncloudGroupNotFoundException
   *           Group doesn&apos;t exist anymore
   */
  void delete(String groupname);

}
