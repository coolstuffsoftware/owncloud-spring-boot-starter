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

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

/**
 * Get Information about Users and Groups from the Owncloud Server.
 *
 * @author mufasa1976
 */
public interface OwncloudUserQueryService {

  /**
   * Find all Users.
   * @return List of all Users
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAllUsers();

  /**
   * Find all Users whose Display Name confirm to the Filter Criterion.
   * @param filter Filter Criterion for Display Name
   * @return List of all Users whose Display Name confirm to the Filter Criterion
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAllUsers(String filter);

  /**
   * Find all Groups.
   * @return List of all Groups
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAllGroups();

  /**
   * Find all Groups whose Name confirm to the Filter Criterion.
   * @param filter Filter Criterion for the Name
   * @return List of all Groups whose Name confirm to the Filter Criterion
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAllGroups(String filter);

  /**
   * Find all Members of a Group
   * @param group Name of the Group
   * @return List of all Users whose are Members of the Group
   * @throws AccessDeniedException Not an Administrator
   * @throws OwncloudGroupNotFoundException Group doesn&apos;t exist
   */
  List<String> findAllMembersOfGroup(String group);

  /**
   * Find all Groups of a User
   * @param username Name of the User
   * @return List of all Groups the User is a Member of
   * @throws AccessDeniedException Neither an Administrator nor the Owner
   * @throws UsernameNotFoundException User doesn&apos;t exist
   */
  List<String> findAllGroupsOfUser(String username);

  /**
   * Get the Details of a User
   * @param username Name of the User
   * @return Details of the User
   * @throws AccessDeniedException Neither an Administrator nor the Owner
   * @throws UsernameNotFoundException User doesn&apos;t exist
   */
  OwncloudUserDetails findOneUser(String username);

}
