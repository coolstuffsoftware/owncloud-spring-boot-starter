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
package software.coolstuff.springframework.owncloud.service.impl.local;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

import java.util.Collection;

interface OwncloudLocalUserDataService {

  OwncloudUserDetails convert(OwncloudLocalUserData.User user, boolean prefixAuthorities);

  boolean userNotExists(String username);

  boolean userExists(String username);

  Collection<OwncloudLocalUserData.User> getUsers();

  OwncloudLocalUserData.User getUser(String username);

  void addUser(OwncloudLocalUserData.User user);

  void removeUser(String username);

  boolean groupNotExists(String groupname);

  boolean groupExists(String groupname);

  Collection<String> getGroups();

  String getGroup(String groupname);

  void addGroup(String group);

  void removeGroup(String groupname);

}
