package software.coolstuff.springframework.owncloud.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

class OwncloudUserModificationServiceImpl extends AbstractOwncloudServiceImpl implements OwncloudUserModificationService {

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  @Autowired
  private OwncloudProperties owncloudProperties;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  OwncloudUserModificationServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public OwncloudUserDetails saveUser(OwncloudModificationUser user) {
    checkModificationsEnabled();

    Validate.notNull(user);
    Validate.notBlank(user.getUsername());

    if (isRestNotAvailable()) {
      return resourceService.saveUser(user);
    }

    try {
      // First check, if the User already exists within the Owncloud
      OcsUserInformation existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), OcsUserInformation.class, user.getUsername());

      // User exists --> update User
      updateUser(user, existingUser.getData());
    } catch (UsernameNotFoundException e) {
      // User doesn't exist --> create User
      createUser(user);
    }

    OwncloudUserDetails foundUserDetails = userQueryService.findOneUser(user.getUsername());
    foundUserDetails.setPassword(user.getPassword());
    return foundUserDetails;
  }

  private void checkModificationsEnabled() {
    if (!owncloudProperties.isEnableModifications()) {
      throw new AccessDeniedException("no modifications allowed");
    }
  }

  private void updateUser(OwncloudModificationUser user, OcsUserInformation.User existingUser) {
    // change the Display Name
    if (!StringUtils.equals(user.getDisplayName(), existingUser.getDisplayname())) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.DISPLAY_NAME, user.getDisplayName());
    }

    // change the eMail
    if (!StringUtils.equals(user.getEmail(), existingUser.getEmail())) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.EMAIL, user.getEmail());
    }

    // change the availability Status
    if (user.isEnabled() != existingUser.isEnabled()) {
      changeOwncloudUserAvailabilityStatus(user.getUsername(), user.isEnabled());
    }

    manageGroupMemberships(user.getUsername(), user.getGroups());
  }

  private void updateOwncloudUserField(String username, UserUpdateField updateField, String value) {
    Map<String, List<String>> data = new HashMap<>();
    data.put("key", Lists.newArrayList(updateField.getFieldName()));
    data.put("value", Lists.newArrayList(value));

    exchange("/cloud/users/{user}", HttpMethod.PUT, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new UsernameNotFoundException(username);
        case 102:
          throw new IllegalStateException(metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, username);
  }

  private void changeOwncloudUserAvailabilityStatus(String username, boolean status) {
    exchange("/cloud/users/{user}/{status}", HttpMethod.PUT, emptyEntity(), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new UsernameNotFoundException(username);
        case 102:
          throw new IllegalStateException(metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, username, status ? "enable" : "disable");
  }

  private void manageGroupMemberships(String username, List<String> expectedGroups) {
    OcsGroups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class, username);
    List<String> actualGroups = OwncloudUserQueryServiceImpl.convertOcsGroups(ocsGroups);

    // add new Group Memberships
    if (CollectionUtils.isNotEmpty(expectedGroups)) {
      for (String group : expectedGroups) {
        if (actualGroups.contains(group)) {
          actualGroups.remove(group);
          continue;
        }

        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(group));

        exchange("/cloud/users/{user}/groups", HttpMethod.POST, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
          if ("ok".equals(metaInformation.getStatus())) {
            return;
          }

          switch (metaInformation.getStatuscode()) {
            case 100:
              return;
            case 101:
              throw new IllegalArgumentException(metaInformation.getMessage());
            case 102:
              throw new OwncloudGroupNotFoundException(group);
            case 103:
              throw new UsernameNotFoundException(username);
            case 104:
              throw new AccessDeniedException("Not authorized to add a Group " + group + " to User " + username);
            case 105:
              throw new IllegalStateException("Error while adding Group " + group + " to User " + username + ". Reason: " + metaInformation.getMessage());
            case 997:
              throw new AccessDeniedException("Not Authorized to access Resource " + uri);
            default:
              throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
          }
        }, username);
      }
    }

    // remove Group Memberships
    if (CollectionUtils.isNotEmpty(actualGroups)) {
      for (String removableGroup : actualGroups) {
        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(removableGroup));

        exchange("/cloud/users/{user}/groups", HttpMethod.DELETE, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
          if ("ok".equals(metaInformation.getStatus())) {
            return;
          }

          switch (metaInformation.getStatuscode()) {
            case 100:
              return;
            case 101:
              throw new IllegalArgumentException(metaInformation.getMessage());
            case 102:
              throw new OwncloudGroupNotFoundException(removableGroup);
            case 103:
              throw new UsernameNotFoundException(username);
            case 104:
              throw new AccessDeniedException("Not authorized to remove Group " + removableGroup + " from User " + username);
            case 105:
              throw new IllegalStateException("Error while removing Group " + removableGroup + " from User " + username + ". Reason: " + metaInformation.getMessage());
            case 997:
              throw new AccessDeniedException("Not Authorized to access Resource " + uri);
            default:
              throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
          }
        }, username);
      }
    }
  }

  private void createUser(OwncloudModificationUser user) {
    Validate.notBlank(user.getPassword());

    Map<String, List<String>> data = new HashMap<>();
    data.put("userid", Lists.newArrayList(user.getUsername()));
    data.put("password", Lists.newArrayList(user.getPassword()));

    exchange("/cloud/users", HttpMethod.POST, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new IllegalArgumentException(metaInformation.getMessage());
        case 102:
          throw new OwncloudUsernameAlreadyExistsException(user.getUsername());
        case 103:
          throw new IllegalStateException(metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    });

    OcsUserInformation existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), OcsUserInformation.class, user.getUsername());
    updateUser(user, existingUser.getData());
  }

  @Override
  public void deleteUser(String username) {
    checkModificationsEnabled();

    Validate.notBlank(username);

    if (isRestNotAvailable()) {
      resourceService.deleteUser(username);
      return;
    }

    exchange("/cloud/users/{user}", HttpMethod.DELETE, emptyEntity(), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new UsernameNotFoundException(username);
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, username);
  }

  @Override
  public void createGroup(String groupname) {
    checkModificationsEnabled();

    Validate.notBlank(groupname);

    if (isRestNotAvailable()) {
      resourceService.createGroup(groupname);
      return;
    }

    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    exchange("/cloud/groups/{group}", HttpMethod.POST, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new IllegalArgumentException(metaInformation.getMessage());
        case 102:
          throw new OwncloudGroupAlreadyExistsException(groupname);
        case 103:
          throw new IllegalStateException("Failed to add Group " + groupname + ". Reason: " + metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, groupname);
  }

  @Override
  public void deleteGroup(String groupname) {
    checkModificationsEnabled();

    Validate.notBlank(groupname);

    if (isRestNotAvailable()) {
      resourceService.deleteGroup(groupname);
      return;
    }

    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    exchange("/cloud/groups/{group}", HttpMethod.DELETE, multiValuedEntity(data), OcsVoid.class, (uri, metaInformation) -> {
      if ("ok".equals(metaInformation.getStatus())) {
        return;
      }

      switch (metaInformation.getStatuscode()) {
        case 100:
          return;
        case 101:
          throw new OwncloudGroupNotFoundException(groupname);
        case 102:
          throw new IllegalStateException("Failed to delete Group " + groupname + ". Reason: " + metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    }, groupname);
  }

  private static enum UserUpdateField {
    DISPLAY_NAME("display"),
    EMAIL("email"),
    PASSWORD("password");

    private final String fieldName;

    private UserUpdateField(final String fieldName) {
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

}
