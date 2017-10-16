/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.CheckOwncloudModification;

@Slf4j
public class OwncloudRestUserServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudRestUserServiceExtension {

  OwncloudRestUserServiceImpl(RestTemplateBuilder builder, OwncloudRestProperties properties) {
    super(builder, properties);
  }

  @Override
  public Optional<OwncloudUserDetails> findOne(String username) {
    try {
      return Optional.of(findOneWithExceptionWhenNotFound(username));
    } catch (UsernameNotFoundException ignored) {
      return Optional.empty();
    }
  }

  private OwncloudUserDetails findOneWithExceptionWhenNotFound(String username) {
    Validate.notBlank(username);
    log.debug("Get Information about User {} from Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    log.debug("Get all Groups assigned to User {} from Location {}", username, getLocation());
    Ocs.Groups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return convert(username, user, groups);
  }

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    Ocs.Users users = null;
    if (StringUtils.isBlank(filter)) {
      log.debug("Get all Users by Filter Criteria {} from Location {}", filter, getLocation());
      users = exchange("/cloud/users", HttpMethod.GET, emptyEntity(), Ocs.Users.class);
    } else {
      log.debug("Get all Users from Location {}", getLocation());
      users = exchange("/cloud/users?search={filter}", HttpMethod.GET, emptyEntity(), Ocs.Users.class, filter);
    }
    return convertUsers(users);
  }

  private List<String> convertUsers(Ocs.Users ocsUsers) {
    List<String> users = new ArrayList<>();
    if (isUsersNotNull(ocsUsers)) {
      for (Ocs.Users.Data.Element element : ocsUsers.getData().getUsers()) {
        log.trace("Add User {} to the Result List", element.getElement());
        users.add(element.getElement());
      }
    }
    return users;
  }

  private boolean isUsersNotNull(Ocs.Users ocsUsers) {
    return ocsUsers != null && ocsUsers.getData() != null && ocsUsers.getData().getUsers() != null;
  }

  @Override
  public OwncloudRestQuotaImpl getQuota(String username) {
    log.debug("Get Information about User {} from Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    Ocs.User.Data.Quota quota = user.getData().getQuota();
    return OwncloudRestQuotaImpl.builder()
        .username(username)
        .free(quota.getFree())
        .used(quota.getUsed())
        .total(quota.getTotal())
        .relative(quota.getRelative())
        .build();
  }

  @Override
  @CheckOwncloudModification
  public OwncloudUserDetails save(OwncloudModificationUser user) {
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

    return findOneWithExceptionWhenNotFound(user.getUsername());
  }

  private void updateUser(OwncloudModificationUser user, Ocs.User.Data existingUser) {
    // change the Display Name
    if (isDisplaynameChanged(user, existingUser)) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.DISPLAY_NAME, user.getDisplayname());
    }

    // change the eMail
    if (isEmailChanged(user, existingUser)) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.EMAIL, user.getEmail());
    }

    // change the Quota
    if (isQuotaChanged(user, existingUser)) {
      updateOwncloudUserField(user.getUsername(), UserUpdateField.QUOTA, user.getQuota());
    }

    // change the availability Status
    if (user.isEnabled() != existingUser.isEnabled()) {
      changeOwncloudUserAvailabilityStatus(user.getUsername(), user.isEnabled());
    }

    manageGroupMemberships(user.getUsername(), user.getGroups());
  }

  private boolean isDisplaynameChanged(OwncloudModificationUser user, Ocs.User.Data existingUser) {
    return !StringUtils.equals(user.getDisplayname(), existingUser.getDisplayname());
  }

  private void updateOwncloudUserField(String username, UserUpdateField updateField, Object value) {
    log.trace("Create the Message Body for the Change Request of the Attribute {} of User {} on Location {}",
        updateField, username, getLocation());
    Map<String, List<String>> data = new HashMap<>();
    data.put("key", Lists.newArrayList(updateField.getFieldName()));
    if (value != null) {
      data.put("value", Lists.newArrayList(updateField.format(value)));
    }

    log.debug("Update Attribute {} of User {} on Location {}", updateField, username, getLocation());
    exchange(
        "/cloud/users/{user}",
        HttpMethod.PUT,
        multiValuedEntity(data),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkFieldUpdate(authenticatedUser, uri, meta, username),
        username);
  }

  private void checkFieldUpdate(String authenticatedUser, String uri, Ocs.Meta meta, String username) {
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
      case 103:
        log.error("Error 103: {}", meta.getMessage());
        throw new IllegalStateException(meta.getMessage());
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  private boolean isEmailChanged(OwncloudModificationUser user, Ocs.User.Data existingUser) {
    return !StringUtils.equals(user.getEmail(), existingUser.getEmail());
  }

  private boolean isQuotaChanged(OwncloudModificationUser user, Ocs.User.Data existingUser) {
    Long newQuota = user.getQuota();
    Long existingQuota = existingUser.getQuota() != null ? existingUser.getQuota().getTotal() : null;
    return ObjectUtils.compare(newQuota, existingQuota) != 0;
  }

  private void changeOwncloudUserAvailabilityStatus(String username, boolean status) {
    log.debug("{} User {} on Location {}", status ? "Enable" : "Disable", username, getLocation());
    exchange(
        "/cloud/users/{user}/{status}",
        HttpMethod.PUT,
        emptyEntity(),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkAvailabilityStatusUpdate(authenticatedUser, uri, meta, username),
        username,
        status ? "enable" : "disable");
  }

  private void checkAvailabilityStatusUpdate(String authenticatedUser, String uri, Ocs.Meta meta, String username) {
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
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", username, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
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
        exchange(
            "/cloud/users/{user}/groups",
            HttpMethod.POST,
            multiValuedEntity(data),
            Ocs.Void.class,
            (authenticatedUser, uri, meta) -> checkAssignGroupMembership(authenticatedUser, uri, meta, username, groupname),
            username);
      }
    }

    // remove Group Memberships
    if (CollectionUtils.isNotEmpty(actualGroups)) {
      for (String groupname : actualGroups) {
        log.trace("Create Message Body for unassign Group {} from User {}", groupname, username);
        Map<String, List<String>> data = new HashMap<>();
        data.put("groupid", Lists.newArrayList(groupname));

        log.debug("Remove Group {} from User {} on Location {}", groupname, username, getLocation());
        exchange(
            "/cloud/users/{user}/groups",
            HttpMethod.DELETE,
            multiValuedEntity(data),
            Ocs.Void.class,
            (authenticatedUser, uri, meta) -> checkRemoveGroupMembership(authenticatedUser, uri, meta, username, groupname),
            username);
      }
    }
  }

  private void checkAssignGroupMembership(String authenticatedUser, String uri, Ocs.Meta meta, String username, String groupname) {
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
        exceptionMessage = String.format("User %s is not authorized to assign Group %s to User %s", username, groupname, username);
        log.warn("Error 104: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      case 105:
        exceptionMessage = String.format("Error while assign Group %s to User %s. Reason: %s", groupname, username, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error("Error 105: {}", exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", username, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  private void checkRemoveGroupMembership(String authenticatedUser, String uri, Ocs.Meta meta, String username, String groupname) {
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
        exceptionMessage = String.format("User %s is not authorized to unassign Group %s from User %s", authenticatedUser, groupname, username);
        log.warn("Error 104: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      case 105:
        exceptionMessage = String.format("Error while unassign Group %s from User %s. Reason: %s", groupname, username, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error("Error 105: {}", exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }

  }

  private void createUser(OwncloudModificationUser user) {
    Validate.notBlank(user.getPassword());

    log.trace("Create the Message Body for the Creation Request of User {}", user.getUsername());
    Map<String, List<String>> data = new HashMap<>();
    data.put("userid", Lists.newArrayList(user.getUsername()));
    data.put("password", Lists.newArrayList(user.getPassword()));

    log.debug("Create User {}", user.getUsername());
    exchange(
        "/cloud/users",
        HttpMethod.POST,
        multiValuedEntity(data),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkCreateUser(authenticatedUser, uri, meta, user.getUsername()));
    log.info("User {} successfully created", user.getUsername());

    log.debug("Re-Read the Information about User {} from Location {}", user.getUsername(), getLocation());
    Ocs.User existingUser = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, user.getUsername());
    updateUser(user, existingUser.getData());
  }

  private void checkCreateUser(String authenticatedUser, String uri, Ocs.Meta meta, String username) {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    String exceptionMessage;
    switch (meta.getStatuscode()) {
      case 101:
        log.error("Error 101: {}", meta.getMessage());
        throw new IllegalArgumentException(meta.getMessage());
      case 102:
        log.warn("Error 102: User {} already exists", username);
        throw new OwncloudUsernameAlreadyExistsException(username);
      case 103:
        log.error("Error 103: {}", meta.getMessage());
        throw new IllegalStateException(meta.getMessage());
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  @Override
  @CheckOwncloudModification
  public void delete(String username) {
    Validate.notBlank(username);

    log.debug("Delete User {} from Location {}", username, getLocation());
    exchange(
        "/cloud/users/{user}",
        HttpMethod.DELETE,
        emptyEntity(),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkDeleteUser(authenticatedUser, uri, meta, username),
        username);
    log.info("User {} successfully removed from Location {}", username, getLocation());
  }

  private void checkDeleteUser(String authenticatedUser, String uri, Ocs.Meta meta, String username) {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    String exceptionMessage;
    switch (meta.getStatuscode()) {
      case 101:
        log.error("Error 101: User {} not found", username);
        throw new UsernameNotFoundException(username);
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  private enum UserUpdateField {
    DISPLAY_NAME("display"),
    EMAIL("email"),
    PASSWORD("password"),
    QUOTA("quota", new DecimalFormat("###########0"));

    private final String fieldName;
    private Format format;

    UserUpdateField(final String fieldName) {
      this.fieldName = fieldName;
    }

    UserUpdateField(final String fieldName, final Format format) {
      this(fieldName);
      this.format = format;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String format(Object value) {
      if (format == null) {
        return value != null ? value.toString() : null;
      }
      return format.format(value);
    }
  }

}
