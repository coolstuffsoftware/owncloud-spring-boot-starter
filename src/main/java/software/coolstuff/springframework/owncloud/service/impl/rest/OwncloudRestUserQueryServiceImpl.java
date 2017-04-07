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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.rest.Ocs.User.Data.Quota;

@Slf4j
class OwncloudRestUserQueryServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudRestUserQueryService {

  OwncloudRestUserQueryServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public OwncloudUserDetails findOne(String username) {
    Validate.notBlank(username);
    log.debug("Get Information about User {} from Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    log.debug("Get all Groups assigned to User {} from Location {}", username, getLocation());
    Ocs.Groups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return convert(username, user, groups);
  }

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    Ocs.Users users = null;
    if (StringUtils.isBlank(filter)) {
      log.debug("Get all Users by Filter Criteria {} from Location {}", filter, getLocation());
      users = exchange("/cloud/users", HttpMethod.GET, emptyEntity(), Ocs.Users.class);
    } else {
      log.debug("Get all Users from Location {}", getLocation());
      users = exchange("/cloud/users?search={filter}", HttpMethod.GET, emptyEntity(), Ocs.Users.class, filter);
    }
    return convertUsers(users);
  }

  private List<String> convertUsers(Ocs.Users ocsUsers) {
    List<String> users = new ArrayList<>();
    if (isUsersNotNull(ocsUsers)) {
      for (Ocs.Users.Data.Element element : ocsUsers.getData().getUsers()) {
        log.trace("Add User {} to the Result List", element.getElement());
        users.add(element.getElement());
      }
    }
    return users;
  }

  private boolean isUsersNotNull(Ocs.Users ocsUsers) {
    return ocsUsers != null && ocsUsers.getData() != null && ocsUsers.getData().getUsers() != null;
  }

  @Override
  public OwncloudRestQuota getQuota(String username) {
    log.debug("Get Information about User {} from Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    Quota quota = user.getData().getQuota();
    return OwncloudRestQuota.builder()
        .username(username)
        .free(quota.getFree())
        .used(quota.getUsed())
        .total(quota.getTotal())
        .relative(quota.getRelative())
        .build();
  }
}
