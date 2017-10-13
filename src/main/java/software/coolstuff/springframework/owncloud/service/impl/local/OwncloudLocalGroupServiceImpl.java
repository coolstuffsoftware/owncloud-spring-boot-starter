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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.impl.CheckOwncloudModification;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OwncloudLocalGroupServiceImpl extends AbstractOwncloudLocalUserAndGroupServiceImpl implements OwncloudGroupService {

  public OwncloudLocalGroupServiceImpl(OwncloudLocalUserDataService localUserDataService) {
    super(localUserDataService);
  }

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    log.debug("Get all Users with a Group like {}", filter);
    List<String> filteredGroups = new ArrayList<>();
    for (String group : getLocalUserDataService().getGroups()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(group, filter)) {
        log.trace("add Group {} to the Result", group);
        filteredGroups.add(group);
      }
    }
    return filteredGroups;
  }

  @Override
  public List<String> findAllUsers(String groupname) {
    Validate.notBlank(groupname);
    if (getLocalUserDataService().groupNotExists(groupname)) {
      log.error("Group {} not found", groupname);
      throw new OwncloudGroupNotFoundException(groupname);
    }

    log.debug("Get all Members of Group {}", groupname);
    List<String> members = new ArrayList<>();
    for (OwncloudLocalUserData.User user : getLocalUserDataService().getUsers()) {
      addWhenMemberOfGroup(groupname, members, user);
    }
    return members;
  }

  private void addWhenMemberOfGroup(String groupname, List<String> members, OwncloudLocalUserData.User user) {
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (String group : user.getGroups()) {
        if (StringUtils.equals(groupname, group)) {
          log.trace("Add User {} as a Member of Group {}", user.getUsername(), groupname);
          members.add(user.getUsername());
          break;
        }
      }
    }
  }

  @Override
  public List<String> findAllGroups(String username) {
    OwncloudLocalUserData.User user = getCheckedUser(username);
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      log.debug("Get all Groups assigned to User {}", user.getUsername());
      for (String group : user.getGroups()) {
        log.trace("Add Group {} to be assigned to User {}", group, user.getUsername());
        groups.add(group);
      }
    }
    return groups;
  }

  @Override
  @CheckOwncloudModification
  public void create(String groupname) {
    Validate.notBlank(groupname);
    if (getLocalUserDataService().groupExists(groupname)) {
      log.error("Group {} already exists", groupname);
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    log.debug("Create Group {}", groupname);
    getLocalUserDataService().addGroup(groupname);
    log.info("Group {} successfully created");
  }

  @Override
  @CheckOwncloudModification
  public void delete(String groupname) {
    Validate.notBlank(groupname);

    log.debug("Get Information of Group {} from the Resource Service", groupname);
    String group = getLocalUserDataService().getGroup(groupname);
    if (StringUtils.isBlank(group)) {
      throw new OwncloudGroupNotFoundException(groupname);
    }

    for (OwncloudLocalUserData.User user : getLocalUserDataService().getUsers()) {
      if (user.getGroups() != null) {
        log.trace("Revoke Assignment of Group {} from User {}", groupname, user.getUsername());
        user.getGroups().remove(group);
      }
    }
    log.debug("Remove Group {}", groupname);
    getLocalUserDataService().removeGroup(groupname);
    log.info("Group {} successfully removed", groupname);
  }

}
