/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.api;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;

import java.util.List;

/**
 * Get and modify Information of Groups on the Owncloud Server.
 * <p/>
 * This Service is only usable when the User has been authenticated
 * by the OwncloudAuthenticationProvider.
 *
 * @author mufasa1976
 * @since 1.0.0
 */
public interface OwncloudGroupService {

  /**
   * Find all Groups.
   * @return List of all Groups
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAll();

  /**
   * Find all Groups whose Name confirm to the Filter Criterion.
   * @param filter Filter Criterion for the Name
   * @return List of all Groups whose Name confirm to the Filter Criterion
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAll(String filter);

  /**
   * Find all Users that are members of a Group
   * @param groupname Name of the Group
   * @return List of all Users that are Members of the Group
   * @throws AccessDeniedException Not an Administrator
   * @throws OwncloudGroupNotFoundException Group doesn&apos;t exist
   */
  List<String> findAllUsers(String groupname);

  /**
   * Find all Groups of a User
   * @param username Name of the User
   * @return List of all Groups the User is a Member of
   * @throws AccessDeniedException Neither an Administrator nor the Owner
   * @throws UsernameNotFoundException User doesn&apos;t exist
   */
  List<String> findAllGroups(String username);

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
