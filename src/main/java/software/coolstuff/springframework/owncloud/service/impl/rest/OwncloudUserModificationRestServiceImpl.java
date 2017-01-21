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
package software.coolstuff.springframework.owncloud.service.impl.rest;

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

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@Slf4j
class OwncloudUserModificationRestServiceImpl extends AbstractOwncloudRestServiceImpl
    implements OwncloudUserModificationService {

  @Autowired
  private OwncloudUserQueryService userQueryService;

  OwncloudUserModificationRestServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public OwncloudUserDetails saveUser(OwncloudModificationUser user) {
    Validate.notNull(user);
    Validate.notBlank(user.getUsername());

    try {
      // First check, if the User already exists within the Owncloud
      log.debug("Check, if the User {} exists at Location {}", user.getUsername(), getLocation());
      Ocs.User existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, user.getUsername());

      // User exists --> update User
      updateUser(user, existingUser.getData());
    } catch (UsernameNotFoundException e) {
      // User doesn't exist --> create User
      createUser(user);
    }

    OwncloudUserDetails foundUserDetails = userQueryService.findOneUser(user.getUsername());
    return foundUserDetails;
  }

  private void updateUser(OwncloudModificationUser user, Ocs.User.Data existingUser) {
    // change the Display Name
    if (!StringUtils.equals(user.getDisplayname(), existingUser.getDisplayname())) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.DISPLAY_NAME, user.getDisplayname());
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
    log.trace("Create the Message Body for the Change Request of the Attribute {} of User {} on Location {}",
        updateField, username, getLocation());
    Map<String, List<String>> data = new HashMap<>();
    data.put("key", Lists.newArrayList(updateField.getFieldName()));
    data.put("value", Lists.newArrayList(value));

    log.debug("Update Attribute {} of User {} on Location {}", updateField, username, getLocation());
    exchange("/cloud/users/{user}", HttpMethod.PUT, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.warn("Error 101: User {} not found", username);
          throw new IllegalStateException("User " + username + " not found");
        case 102:
          log.error("Error 102: {}", meta.getMessage());
          throw new IllegalStateException(meta.getMessage());
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    }, username);
  }

  private void changeOwncloudUserAvailabilityStatus(String username, boolean status) {
    log.debug("{} User {} on Location {}", status ? "Enable" : "Disable", username, getLocation());
    exchange("/cloud/users/{user}/{status}", HttpMethod.PUT, emptyEntity(), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.error("Error 101: User {} not found", username);
          throw new IllegalStateException("User " + username + " not found");
        case 102:
          log.error("Error 102: {}", meta.getMessage());
          throw new IllegalStateException(meta.getMessage());
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    }, username, status ? "enable" : "disable");
  }

  private void manageGroupMemberships(String username, List<String> expectedGroups) {
    log.debug("Get the existing Group Memberships of User {} from Location {}", username, getLocation());
    Ocs.Groups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    List<String> actualGroups = OwncloudRestUtils.convertGroups(ocsGroups);

    // add new Group Memberships
    if (CollectionUtils.isNotEmpty(expectedGroups)) {
      for (String groupname : expectedGroups) {
        if (actualGroups.contains(groupname)) {
          log.trace("Group {} is already assigned to User {}", groupname, username);
          actualGroups.remove(groupname);
          continue;
        }

        log.trace("Create Message Body for assign Group {} to User {}", groupname, username);
        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(groupname));

        log.debug("Assign Group {} to User {} on Location {}", groupname, username, getLocation());
        exchange("/cloud/users/{user}/groups", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
          if ("ok".equals(meta.getStatus())) {
            return;
          }

          String exceptionMessage;
          switch (meta.getStatuscode()) {
            case 101:
              log.error("Error 101: {}", meta.getMessage());
              throw new IllegalArgumentException(meta.getMessage());
            case 102:
              log.warn("Error 102: Owncloud Group {} not found", groupname);
              throw new OwncloudGroupNotFoundException(groupname);
            case 103:
              log.warn("Error 103: User {} not found", username);
              throw new IllegalStateException("User " + username + " not found");
            case 104:
              exceptionMessage = String.format("User %s is not authorized to assign Group %s to User %s", authorizationUser, groupname, username);
              log.warn("Error 104: {}", exceptionMessage);
              throw new AccessDeniedException(exceptionMessage);
            case 105:
              exceptionMessage = String.format("Error while assign Group %s to User %s. Reason: %s", groupname, username, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
              log.error("Error 105: {}", exceptionMessage);
              throw new IllegalStateException(exceptionMessage);
            case 997:
              exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
              log.warn("Error 997: {}", exceptionMessage);
              throw new AccessDeniedException(exceptionMessage);
            default:
              exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
              log.error(exceptionMessage);
              throw new IllegalStateException(exceptionMessage);
          }
        }, username);
      }
    }

    // remove Group Memberships
    if (CollectionUtils.isNotEmpty(actualGroups)) {
      for (String groupname : actualGroups) {
        log.trace("Create Message Body for unassign Group {} from User {}", groupname, username);
        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(groupname));

        log.debug("Unassign Group {} from User {} on Location {}", groupname, username, getLocation());
        exchange("/cloud/users/{user}/groups", HttpMethod.DELETE, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
          if ("ok".equals(meta.getStatus())) {
            return;
          }

          String exceptionMessage;
          switch (meta.getStatuscode()) {
            case 101:
              log.error("Error 101: {}", meta.getMessage());
              throw new IllegalArgumentException(meta.getMessage());
            case 102:
              log.warn("Error 102: Owncloud Group {} not found", groupname);
              throw new OwncloudGroupNotFoundException(groupname);
            case 103:
              log.warn("Error 103: User {} not found", username);
              throw new IllegalStateException("User " + username + " not found");
            case 104:
              exceptionMessage = String.format("User %s is not authorized to unassign Group %s from User %s", authorizationUser, groupname, username);
              log.warn("Error 104: {}", exceptionMessage);
              throw new AccessDeniedException(exceptionMessage);
            case 105:
              exceptionMessage = String.format("Error while unassign Group %s from User %s. Reason: %s", groupname, username, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
              log.error("Error 105: {}", exceptionMessage);
              throw new IllegalStateException(exceptionMessage);
            case 997:
              exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
              log.warn("Error 997: {}", exceptionMessage);
              throw new AccessDeniedException(exceptionMessage);
            default:
              exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
              log.error(exceptionMessage);
              throw new IllegalStateException(exceptionMessage);
          }
        }, username);
      }
    }
  }

  private void createUser(OwncloudModificationUser user) {
    Validate.notBlank(user.getPassword());

    log.trace("Create the Message Body for the Creation Request of User {}", user.getUsername());
    Map<String, List<String>> data = new HashMap<>();
    data.put("userid", Lists.newArrayList(user.getUsername()));
    data.put("password", Lists.newArrayList(user.getPassword()));

    log.debug("Create User {}", user.getUsername());
    exchange("/cloud/users", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.error("Error 101: {}", meta.getMessage());
          throw new IllegalArgumentException(meta.getMessage());
        case 102:
          log.warn("Error 102: User {} already exists", user.getUsername());
          throw new OwncloudUsernameAlreadyExistsException(user.getUsername());
        case 103:
          log.error("Error 103: {}", meta.getMessage());
          throw new IllegalStateException(meta.getMessage());
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    });
    log.info("User {} successfully created", user.getUsername());

    log.debug("Re-Read the Information about User {} from Location {}", user.getUsername(), getLocation());
    Ocs.User existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, user.getUsername());
    updateUser(user, existingUser.getData());
  }

  @Override
  public void deleteUser(String username) {
    Validate.notBlank(username);

    log.debug("Delete User {} from Location {}", username, getLocation());
    exchange("/cloud/users/{user}", HttpMethod.DELETE, emptyEntity(), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.error("Error 101: User {} not found", username);
          throw new UsernameNotFoundException(username);
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    }, username);
    log.info("User {} successfully removed from Location {}", username, getLocation());
  }

  @Override
  public void createGroup(String groupname) {
    Validate.notBlank(groupname);

    log.trace("Create Message Body for Create Request of Group {}", groupname);
    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    log.debug("Create Group {} on Location {}", groupname, getLocation());
    exchange("/cloud/groups/{group}", HttpMethod.POST, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.error("Error 101: {}", meta.getMessage());
          throw new IllegalArgumentException(meta.getMessage());
        case 102:
          log.warn("Error 102: Group {} already exists", groupname);
          throw new OwncloudGroupAlreadyExistsException(groupname);
        case 103:
          exceptionMessage = String.format("Failed to create Group %s. Reason: %s", groupname, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error("Error 103: {}", exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    }, groupname);
    log.info("Group {} successfully created on Location {}", groupname, getLocation());
  }

  @Override
  public void deleteGroup(String groupname) {
    Validate.notBlank(groupname);

    log.trace("Create Message Body for Delete Request of Group {}", groupname);
    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    log.debug("Delete Group {} on Location {}", groupname, getLocation());
    exchange("/cloud/groups/{group}", HttpMethod.DELETE, multiValuedEntity(data), Ocs.Void.class, (authorizationUser, uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      String exceptionMessage;
      switch (meta.getStatuscode()) {
        case 101:
          log.warn("Error 101: Group {} not exists", groupname);
          throw new OwncloudGroupNotFoundException(groupname);
        case 102:
          exceptionMessage = String.format("Failed to delete Group %s. Reason: %s", groupname, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error("Error 102: {}", exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
        case 997:
          exceptionMessage = String.format("User %s is not authorized to access Resource %s", authorizationUser, uri);
          log.warn("Error 997: {}", exceptionMessage);
          throw new AccessDeniedException(exceptionMessage);
        default:
          exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
          log.error(exceptionMessage);
          throw new IllegalStateException(exceptionMessage);
      }
    }, groupname);
    log.info("Group {} successfully removed from Location {}", groupname, getLocation());
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
