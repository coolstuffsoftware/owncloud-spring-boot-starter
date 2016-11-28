package software.coolstuff.springframework.owncloud.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

class OwncloudUserQueryServiceImpl extends AbstractOwncloudServiceImpl implements OwncloudUserQueryService {

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  OwncloudUserQueryServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public List<String> findAllUsers() {
    return findAllUsers(null);
  }

  @Override
  public List<String> findAllUsers(String filter) {
    if (isRestNotAvailable()) {
      return resourceService.getAllUsers(filter);
    }

    OcsUsers users = null;
    if (StringUtils.isBlank(filter)) {
      users = getForObject("/cloud/users", OcsUsers.class);
    } else {
      users = getForObject("/cloud/users?search={filter}", OcsUsers.class, filter);
    }
    return users.getData().getUsers();
  }

  @Override
  public List<String> findAllGroups() {
    return findAllGroups(null);
  }

  @Override
  public List<String> findAllGroups(String filter) {
    if (isRestNotAvailable()) {
      return resourceService.getAllGroups(filter);
    }

    OcsGroups groups = null;
    if (StringUtils.isBlank(filter)) {
      groups = getForObject("/cloud/groups", OcsGroups.class);
    } else {
      groups = getForObject("/cloud/groups?search={filter}", OcsGroups.class, filter);
    }
    return groups.getData().getGroups();
  }

  @Override
  public List<String> findAllMembersOfGroup(String groupname) {
    Validate.notBlank(groupname);
    if (isRestNotAvailable()) {
      return resourceService.getAllMembersOfGroup(groupname);
    }

    OcsUsers users = getForObject("/cloud/groups/{group}", OcsUsers.class, groupname);
    return users.getData().getUsers();
  }

  @Override
  public OwncloudUserDetails findOneUser(String username) {
    Validate.notBlank(username);
    if (isRestNotAvailable()) {
      return resourceService.getUser(username);
    }

    OcsUserInformation userInformation = getForObject("/cloud/users/{user}", OcsUserInformation.class, username);
    OcsGroups groups = getForObject("/cloud/users/{user}/groups", OcsGroups.class, username);
    return createUserDetails(username, userInformation, groups);
  }

}
