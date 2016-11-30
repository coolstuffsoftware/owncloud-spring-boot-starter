package software.coolstuff.springframework.owncloud.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
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
  public OwncloudUserDetails saveUser(OwncloudUserDetails userDetails) {
    checkModificationEnabled();

    Validate.notNull(userDetails);
    Validate.notBlank(userDetails.getUsername());

    if (isRestNotAvailable()) {
      return resourceService.saveUser(userDetails);
    }

    try {
      // First check, if the User already exists within the Owncloud
      OcsUserInformation userInformation = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), OcsUserInformation.class, userDetails.getUsername());

      // User exists --> update User
      updateUser(userDetails, userInformation.getData(), true);
    } catch (UsernameNotFoundException e) {
      // User doesn't exist --> create User
      createUser(userDetails);
    }

    OwncloudUserDetails foundUserDetails = userQueryService.findOneUser(userDetails.getUsername());
    foundUserDetails.setPassword(userDetails.getPassword());
    return foundUserDetails;
  }

  private void checkModificationEnabled() {
    if (!owncloudProperties.isEnableModification()) {
      throw new AccessDeniedException("no modifications allowed");
    }
  }

  private void updateUser(OwncloudUserDetails expectedDetails, OcsUserInformation.User actualDetails, boolean updatePassword) {
    // change the Display Name
    if (!StringUtils.equals(expectedDetails.getDisplayName(), actualDetails.getDisplayname())) {
      updateOwncloudUserField(expectedDetails.getUsername(), UserUpdateField.DISPLAY_NAME, expectedDetails.getDisplayName());
    }

    // change the eMail
    if (!StringUtils.equals(expectedDetails.getEmail(), actualDetails.getEmail())) {
      updateOwncloudUserField(expectedDetails.getUsername(), UserUpdateField.EMAIL, expectedDetails.getEmail());
    }

    // change the availability Status
    if (expectedDetails.isEnabled() != actualDetails.isEnabled()) {
      changeOwncloudUserAvailabilityStatus(expectedDetails.getUsername(), expectedDetails.isEnabled());
    }

    // change the Password
    if (updatePassword && StringUtils.isNotBlank(expectedDetails.getPassword())) {
      updateOwncloudUserField(expectedDetails.getUsername(), UserUpdateField.PASSWORD, expectedDetails.getPassword());
    }

    manageGroupMemberships(expectedDetails.getUsername(), expectedDetails.getAuthorities());
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

  private void manageGroupMemberships(String username, Collection<? extends GrantedAuthority> expectedAuthorities) {
    OcsGroups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), OcsGroups.class, username);
    List<String> actualGroups = ocsGroups.getData().getGroups();

    // add new Group Memberships
    for (GrantedAuthority authority : expectedAuthorities) {
      if (actualGroups.contains(authority.getAuthority())) {
        actualGroups.remove(authority.getAuthority());
        continue;
      }

      Map<String, List<String>> data = new HashMap<>();
      data.put("groupid", Lists.newArrayList(authority.getAuthority()));

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
            throw new OwncloudGroupNotFoundException(authority.getAuthority());
          case 103:
            throw new UsernameNotFoundException(username);
          case 104:
            throw new AccessDeniedException("Not authorized to add a Group " + authority.getAuthority() + " to User " + username);
          case 105:
            throw new IllegalStateException("Error while adding Group " + authority.getAuthority() + " to User " + username + ". Reason: " + metaInformation.getMessage());
          case 997:
            throw new AccessDeniedException("Not Authorized to access Resource " + uri);
          default:
            throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
        }
      }, username);
    }

    // remove Group Memberships
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

  private void createUser(OwncloudUserDetails userDetails) {
    Validate.notBlank(userDetails.getPassword());

    Map<String, List<String>> data = new HashMap<>();
    data.put("userid", Lists.newArrayList(userDetails.getUsername()));
    data.put("password", Lists.newArrayList(userDetails.getPassword()));

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
          throw new OwncloudUsernameAlreadyExistsException(userDetails.getUsername());
        case 103:
          throw new IllegalStateException(metaInformation.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
      }
    });

    OcsUserInformation userInformation = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), OcsUserInformation.class, userDetails.getUsername());
    updateUser(userDetails, userInformation.getData(), false);
  }

  @Override
  public void deleteUser(String username) {
    checkModificationEnabled();

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
    checkModificationEnabled();

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
    checkModificationEnabled();

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
