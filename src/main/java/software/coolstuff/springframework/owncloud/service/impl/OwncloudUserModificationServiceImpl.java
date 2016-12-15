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

class OwncloudUserModificationServiceImpl extends AbstractOwncloudServiceImpl
    implements OwncloudUserModificationService {

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
      Ocs.User existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class,
          user.getUsername());

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

  private void updateUser(OwncloudModificationUser user, Ocs.User.Data existingUser) {
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

    exchange("/cloud/users/{user}", HttpMethod.PUT, multiValuedEntity(data), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new IllegalStateException("Username " + username + " not found");
        case 102:
          throw new IllegalStateException(meta.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
      }
    }, username);
  }

  private void changeOwncloudUserAvailabilityStatus(String username, boolean status) {
    exchange("/cloud/users/{user}/{status}", HttpMethod.PUT, emptyEntity(), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new IllegalStateException("Username " + username + " not found");
        case 102:
          throw new IllegalStateException(meta.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
      }
    }, username, status ? "enable" : "disable");
  }

  private void manageGroupMemberships(String username, List<String> expectedGroups) {
    Ocs.Groups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class,
        username);
    List<String> actualGroups = OwncloudUserQueryServiceImpl.convertGroups(ocsGroups);

    // add new Group Memberships
    if (CollectionUtils.isNotEmpty(expectedGroups)) {
      for (String group : expectedGroups) {
        if (actualGroups.contains(group)) {
          actualGroups.remove(group);
          continue;
        }

        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(group));

        exchange("/cloud/users/{user}/groups", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class,
            (uri, meta) -> {
              if ("ok".equals(meta.getStatus())) {
                return;
              }

              switch (meta.getStatuscode()) {
                case 101:
                  throw new IllegalArgumentException(meta.getMessage());
                case 102:
                  throw new OwncloudGroupNotFoundException(group);
                case 103:
                  throw new IllegalStateException("Username " + username + " not found");
                case 104:
                  throw new AccessDeniedException("Not authorized to add a Group " + group + " to User " + username);
                case 105:
                  throw new IllegalStateException(
                      "Error while adding Group " + group + " to User " + username + ". Reason: " + meta.getMessage());
                case 997:
                  throw new AccessDeniedException("Not Authorized to access Resource " + uri);
                default:
                  throw new IllegalStateException(
                      "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
              }
            }, username);
      }
    }

    // remove Group Memberships
    if (CollectionUtils.isNotEmpty(actualGroups)) {
      for (String removableGroup : actualGroups) {
        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(removableGroup));

        exchange("/cloud/users/{user}/groups", HttpMethod.DELETE, multiValuedEntity(data), Ocs.Void.class,
            (uri, meta) -> {
              if ("ok".equals(meta.getStatus())) {
                return;
              }

              switch (meta.getStatuscode()) {
                case 101:
                  throw new IllegalArgumentException(meta.getMessage());
                case 102:
                  throw new OwncloudGroupNotFoundException(removableGroup);
                case 103:
                  throw new IllegalStateException("Username " + username + " not found");
                case 104:
                  throw new AccessDeniedException(
                      "Not authorized to remove Group " + removableGroup + " from User " + username);
                case 105:
                  throw new IllegalStateException("Error while removing Group " + removableGroup + " from User "
                      + username + ". Reason: " + meta.getMessage());
                case 997:
                  throw new AccessDeniedException("Not Authorized to access Resource " + uri);
                default:
                  throw new IllegalStateException(
                      "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
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

    exchange("/cloud/users", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new IllegalArgumentException(meta.getMessage());
        case 102:
          throw new OwncloudUsernameAlreadyExistsException(user.getUsername());
        case 103:
          throw new IllegalStateException(meta.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
      }
    });

    Ocs.User existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class,
        user.getUsername());
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

    exchange("/cloud/users/{user}", HttpMethod.DELETE, emptyEntity(), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new UsernameNotFoundException(username);
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
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

    exchange("/cloud/groups/{group}", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new IllegalArgumentException(meta.getMessage());
        case 102:
          throw new OwncloudGroupAlreadyExistsException(groupname);
        case 103:
          throw new IllegalStateException("Failed to add Group " + groupname + ". Reason: " + meta.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
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

    exchange("/cloud/groups/{group}", HttpMethod.DELETE, multiValuedEntity(data), Ocs.Void.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 101:
          throw new OwncloudGroupNotFoundException(groupname);
        case 102:
          throw new IllegalStateException("Failed to delete Group " + groupname + ". Reason: " + meta.getMessage());
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        default:
          throw new IllegalStateException(
              "Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
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
