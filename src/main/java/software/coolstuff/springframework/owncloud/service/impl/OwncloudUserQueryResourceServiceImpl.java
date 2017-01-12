package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.Group;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.User;

@RequiredArgsConstructor
@Slf4j
class OwncloudUserQueryResourceServiceImpl implements OwncloudUserQueryService {

  private final OwncloudResourceService resourceService;

  @Autowired
  private OwncloudUserDetailsConversionService conversionService;

  @Override
  public List<String> findAllUsers() {
    return findAllUsers(null);
  }

  @Override
  public List<String> findAllUsers(String filter) {
    log.debug("Get all Users with a DisplayName like {}", filter);
    List<String> filteredUsers = new ArrayList<>();
    for (User user : resourceService.getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayname(), filter)) {
        log.trace("add User {} to the Result", user.getUsername());
        filteredUsers.add(user.getUsername());
      }
    }
    return filteredUsers;
  }

  @Override
  public List<String> findAllGroups() {
    return findAllGroups(null);
  }

  @Override
  public List<String> findAllGroups(String filter) {
    log.debug("Get all Users with a Group like {}", filter);
    List<String> filteredGroups = new ArrayList<>();
    for (Group group : resourceService.getGroups()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(group.getGroup(), filter)) {
        log.trace("add Group {} to the Result", group.getGroup());
        filteredGroups.add(group.getGroup());
      }
    }
    return filteredGroups;
  }

  @Override
  public List<String> findAllMembersOfGroup(String groupname) {
    Validate.notBlank(groupname);
    if (resourceService.groupNotExists(groupname)) {
      log.error("Group {} not found", groupname);
      throw new OwncloudGroupNotFoundException(groupname);
    }

    log.debug("Get all Members of Group {}", groupname);
    List<String> members = new ArrayList<>();
    for (OwncloudResourceData.User user : resourceService.getUsers()) {
      addWhenMemberOfGroup(groupname, members, user);
    }
    return members;
  }

  private void addWhenMemberOfGroup(String groupname, List<String> members, OwncloudResourceData.User user) {
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        if (StringUtils.equals(groupname, group.getGroup())) {
          log.trace("Add User {} as a Member of Group {}", user.getUsername(), groupname);
          members.add(user.getUsername());
          break;
        }
      }
    }
  }

  @Override
  public List<String> findAllGroupsOfUser(String username) {
    User user = getCheckedUser(username);
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      log.debug("Get all Groups assigned to User {}", user.getUsername());
      for (OwncloudResourceData.Group group : user.getGroups()) {
        log.trace("Add Group {} to be assigned to User {}", group.getGroup(), user.getUsername());
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  private OwncloudResourceData.User getCheckedUser(String username) {
    Validate.notBlank(username);
    log.debug("Get User {} from Resource Service", username);
    OwncloudResourceData.User user = resourceService.getUser(username);
    OwncloudUtils.validateUserNotNull(user, username);
    return user;
  }

  @Override
  public OwncloudUserDetails findOneUser(String username) {
    User user = getCheckedUser(username);
    return conversionService.convert(user);
  }

}
