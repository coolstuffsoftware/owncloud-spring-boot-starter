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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

/**
 * Modify Information of Users and Groups on the Owncloud Server.
 *
 * @author mufasa1976
 */
public interface OwncloudUserModificationService {

  /**
   * Create a new User or Update User Details.
   * 
   * @param userDetails
   *          User Details to be saved
   * @return saved User Details
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties.isEnableModifications()</code> or neither an Administrator nor the Owner
   * @throws OwncloudUsernameAlreadyExistsException
   *           the User already exists (minimal chance to get this Exception)
   * @throws OwncloudGroupNotFoundException
   *           referenced Group doesn&apos;t exist (either while adding or removing Membership)
   */
  OwncloudUserDetails saveUser(OwncloudModificationUser userDetails);

  /**
   * Remove a User.
   * 
   * @param username
   *          Name of the User to be removed
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties#isEnableModifications()</code> or neither an Administrator nor the Owner
   * @throws UsernameNotFoundException
   *           the User doesn&apos;t exist anymore
   */
  void deleteUser(String username);

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
  void createGroup(String groupname);

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
  void deleteGroup(String groupname);

}
