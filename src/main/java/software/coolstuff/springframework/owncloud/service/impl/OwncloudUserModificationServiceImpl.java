package software.coolstuff.springframework.owncloud.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

class OwncloudUserModificationServiceImpl extends AbstractOwncloudServiceImpl implements OwncloudUserModificationService {

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  @Autowired
  private OwncloudProperties owncloudProperties;

  OwncloudUserModificationServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public void saveUser(OwncloudUserDetails userDetails) {
    checkModificationEnabled();

    Validate.notNull(userDetails);
    Validate.notBlank(userDetails.getUsername());

    if (isRestNotAvailable()) {
      // TODO: implement Resource
      return;
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

    // TODO: manage the Group-Memberships
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

  private void checkModificationEnabled() {
    if (!owncloudProperties.isEnableModification()) {
      throw new AccessDeniedException("no modifications allowed");
    }
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
