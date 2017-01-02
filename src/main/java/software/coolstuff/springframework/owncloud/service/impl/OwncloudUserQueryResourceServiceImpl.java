package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.Group;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.User;

@RequiredArgsConstructor
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
    List<String> filteredUsers = new ArrayList<>();
    for (User user : resourceService.getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayName(), filter)) {
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
    List<String> filteredGroups = new ArrayList<>();
    for (Group group : resourceService.getGroups()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(group.getGroup(), filter)) {
        filteredGroups.add(group.getGroup());
      }
    }
    return filteredGroups;
  }

  @Override
  public List<String> findAllMembersOfGroup(String groupname) {
    Validate.notBlank(groupname);
    if (resourceService.groupNotExists(groupname)) {
      throw new OwncloudGroupNotFoundException(groupname);
    }

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
      for (OwncloudResourceData.Group group : user.getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  private OwncloudResourceData.User getCheckedUser(String username) {
    Validate.notBlank(username);
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
