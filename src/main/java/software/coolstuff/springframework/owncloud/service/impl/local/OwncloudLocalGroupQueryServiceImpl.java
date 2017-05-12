package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupQueryService;

@RequiredArgsConstructor
@Slf4j
public class OwncloudLocalGroupQueryServiceImpl implements OwncloudGroupQueryService {

  private final OwncloudLocalUserDataService localDataService;

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    log.debug("Get all Users with a Group like {}", filter);
    List<String> filteredGroups = new ArrayList<>();
    for (String group : localDataService.getGroups()) {
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
    if (localDataService.groupNotExists(groupname)) {
      log.error("Group {} not found", groupname);
      throw new OwncloudGroupNotFoundException(groupname);
    }

    log.debug("Get all Members of Group {}", groupname);
    List<String> members = new ArrayList<>();
    for (OwncloudLocalUserData.User user : localDataService.getUsers()) {
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

  private OwncloudLocalUserData.User getCheckedUser(String username) {
    Validate.notBlank(username);
    log.debug("Get User {} from Resource Service", username);
    OwncloudLocalUserData.User user = localDataService.getUser(username);
    OwncloudLocalUtils.validateUserNotNull(user, username);
    return user;
  }

}
