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
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.Collection;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

interface OwncloudLocalUserDataService {

  OwncloudUserDetails convert(OwncloudLocalUserData.User user);

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
