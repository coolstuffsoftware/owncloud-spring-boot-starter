package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.CollectionUtils;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
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
      users = exchange("/cloud/users", HttpMethod.GET, emptyEntity(), OcsUsers.class);
    } else {
      users = exchange("/cloud/users?search={filter}", HttpMethod.GET, emptyEntity(), OcsUsers.class, filter);
    }
    if (CollectionUtils.isEmpty(users.getData().getUsers())) {
      return new ArrayList<>();
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
      groups = exchange("/cloud/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class);
    } else {
      groups = exchange("/cloud/groups?search={filter}", HttpMethod.GET, emptyEntity(), OcsGroups.class, filter);
    }
    if (CollectionUtils.isEmpty(groups.getData().getGroups())) {
      return new ArrayList<>();
    }
    return groups.getData().getGroups();
  }

  @Override
  public List<String> findAllMembersOfGroup(String groupname) {
    Validate.notBlank(groupname);
    if (isRestNotAvailable()) {
      return resourceService.getAllMembersOfGroup(groupname);
    }

    OcsUsers users = exchange("/cloud/groups/{group}", HttpMethod.GET, emptyEntity(), OcsUsers.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        case 998:
          throw new OwncloudGroupNotFoundException(groupname);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, groupname);

    if (CollectionUtils.isEmpty(users.getData().getUsers())) {
      return new ArrayList<>();
    }
    return users.getData().getUsers();
  }

  @Override
  public List<String> findAllGroupsOfUser(String username) {
    Validate.notBlank(username);
    if (isRestNotAvailable()) {
      return resourceService.getGroupsOfUser(username);
    }

    OcsGroups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class, username);
    if (CollectionUtils.isEmpty(ocsGroups.getData().getGroups())) {
      return new ArrayList<>();
    }
    return ocsGroups.getData().getGroups();
  }

  @Override
  public OwncloudUserDetails findOneUser(String username) {
    Validate.notBlank(username);
    if (isRestNotAvailable()) {
      return resourceService.getUser(username);
    }

    OcsUserInformation userInformation = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), OcsUserInformation.class, username);
    OcsGroups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class, username);
    return createUserDetails(username, userInformation, groups);
  }

}
