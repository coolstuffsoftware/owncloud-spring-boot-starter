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
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceImpl.OcsGroups.Groups.Group;

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

    OcsGroups ocsGroups = null;
    if (StringUtils.isBlank(filter)) {
      ocsGroups = exchange("/cloud/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class);
    } else {
      ocsGroups = exchange("/cloud/groups?search={filter}", HttpMethod.GET, emptyEntity(), OcsGroups.class, filter);
    }
    return convertOcsGroups(ocsGroups);
  }

  public static List<String> convertOcsGroups(OcsGroups ocsGroups) {
    List<String> groups = new ArrayList<>();
    if (isOcsGroupsNotNull(ocsGroups)) {
      for (Group group : ocsGroups.getData().getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  private static boolean isOcsGroupsNotNull(OcsGroups ocsGroups) {
    return ocsGroups != null && ocsGroups.getData() != null && ocsGroups.getData().getGroups() != null;
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
    return convertOcsGroups(ocsGroups);
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
