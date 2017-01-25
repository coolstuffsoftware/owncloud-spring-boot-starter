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

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserModificationServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

public abstract class AbstractOwncloudUserModificationServiceRestTest
    extends AbstractOwncloudUserModificationServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Autowired
  private OwncloudRestProperties properties;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) userModificationService;
  }

  @Override
  public OwncloudRestProperties getProperties() {
    return properties;
  }

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithoutGroups(OwncloudModificationUser newUser) throws Exception {
    prepareModificationRestTest(UserModification.builder().newUser(newUser).build());
  };

  private void prepareModificationRestTest(UserModification userModification) throws IOException {
    if (userModification.getExistingUser() != null) {
      respondUser(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + userModification.getExistingUser().getUsername())
              .build(),
          userModification.getExistingUser().isEnabled(),
          userModification.getExistingUser().getEmail(),
          userModification.getExistingUser().getDisplayname());
    } else {
      respondFailure(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + userModification.getNewUser().getUsername())
              .build(),
          998,
          "The requested user could not be found");

      if (userModification.isErrorCreateUser()) {
        respondFailure(
            RestRequest.builder()
                .method(POST)
                .url("/cloud/users").build(),
            userModification.getErrorCodeCreateUser());
      } else {
        MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
        postData.put("userid", Lists.newArrayList(userModification.getNewUser().getUsername()));
        postData.put("password", Lists.newArrayList(userModification.getNewUser().getPassword()));
        respondSuccess(RestRequest.builder().method(POST).url("/cloud/users").build(), postData);
      }

      respondUser(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + userModification.getNewUser().getUsername())
              .build(),
          true,
          null,
          userModification.getNewUser().getUsername());
    }

    // change the Displayname
    if (userModification.getExistingUser() == null || !StringUtils.equals(userModification.getExistingUser().getDisplayname(), userModification.getNewUser().getDisplayname())) {
      if (userModification.isErrorUpdateDisplayName()) {
        respondFailure(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername())
                .build(),
            userModification.getErrorCodeUpdateDisplayName());
      } else {
        MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
        putData.put("key", Lists.newArrayList("display"));
        putData.put("value", Lists.newArrayList(userModification.getNewUser().getDisplayname()));
        respondSuccess(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername())
                .build(),
            putData);
      }
    }

    // change the eMail
    if (userModification.getExistingUser() == null || !StringUtils.equals(userModification.getExistingUser().getEmail(), userModification.getNewUser().getEmail())) {
      if (userModification.isErrorUpdateEmail()) {
        respondFailure(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername()).build(),
            userModification.getErrorCodeUpdateEmail());
      } else {
        MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
        putData = new LinkedMultiValueMap<>();
        putData.put("key", Lists.newArrayList("email"));
        putData.put("value", Lists.newArrayList(userModification.getNewUser().getEmail()));
        respondSuccess(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername())
                .build(),
            putData);
      }
    }

    // change the availatility (enable/disable)
    if (userModification.getExistingUser() != null
        && userModification.getExistingUser().isEnabled() != userModification.getNewUser().isEnabled()) {
      if (userModification.isErrorEnableDisable()) {
        respondFailure(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/" + (userModification.getNewUser().isEnabled() ? "enable" : "disable"))
                .build(),
            userModification.getErrorCodeEnableDisable());
      } else {
        respondSuccess(
            RestRequest.builder()
                .method(PUT)
                .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/" + (userModification.getNewUser().isEnabled() ? "enable" : "disable"))
                .build());
      }
    }

    List<String> addedGroups = new ArrayList<>();
    List<String> removedGroups = new ArrayList<>();
    if (userModification.getExistingUser() != null && CollectionUtils.isNotEmpty(userModification.getExistingUser().getGroups())) {
      respondGroups(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
              .build(),
          userModification.getExistingUser().getGroups().toArray(new String[] {}));
      addedGroups.addAll(CollectionUtils.subtract(userModification.getNewUser().getGroups(), userModification.getExistingUser().getGroups()));
      removedGroups.addAll(CollectionUtils.subtract(userModification.getExistingUser().getGroups(), userModification.getNewUser().getGroups()));
    } else {
      respondGroups(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
              .build());
      addedGroups.addAll(userModification.getNewUser().getGroups());
    }

    if (userModification.isErrorAddGroup()) {
      respondFailure(
          RestRequest.builder()
              .method(POST)
              .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
              .build(),
          userModification.getErrorCodeAddGroup());
    } else {
      for (String group : addedGroups) {
        MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
        postData = new LinkedMultiValueMap<>();
        postData.put("groupid", Lists.newArrayList(group));
        respondSuccess(
            RestRequest.builder()
                .method(POST)
                .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
                .build(),
            postData);
      }
    }

    if (userModification.isErrorRemoveGroup()) {
      respondFailure(
          RestRequest.builder()
              .method(DELETE)
              .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
              .build(),
          userModification.getErrorCodeRemoveGroup());
    } else {
      for (String group : removedGroups) {
        MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
        postData = new LinkedMultiValueMap<>();
        postData.put("groupid", Lists.newArrayList(group));
        respondSuccess(
            RestRequest.builder()
                .method(DELETE)
                .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
                .build(),
            postData);
      }
    }

    MockRestServiceServer queryServer = createServer((OwncloudRestUserQueryServiceImpl) userQueryService);
    respondUser(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + userModification.getNewUser().getUsername())
            .build(),
        userModification.getNewUser().isEnabled(),
        userModification.getNewUser().getEmail(),
        userModification.getNewUser().getDisplayname());
    respondGroups(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + userModification.getNewUser().getUsername() + "/groups")
            .build(),
        CollectionUtils.isEmpty(userModification.getNewUser().getGroups()) ? new String[] {} : userModification.getNewUser().getGroups().toArray(new String[] {}));
  }

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithGroups(OwncloudModificationUser newUser) throws Exception {
    prepareModificationRestTest(
        UserModification.builder()
            .newUser(newUser)
            .build());
  }

  @Override
  protected void prepareTestSaveUser_UpdateUser_OK_WithoutGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {
    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .build());
  }

  @Override
  protected void prepareTestSaveUser_UpdateUser_OK_WithGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {
    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .build());
  }

  @Override
  protected void prepareTestDeleteUser_OK(String username) throws Exception {
    respondSuccess(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/users/" + username)
            .build());
  }

  @Override
  protected void prepareTestDeleteUser_NOK_UsernameNotFoundException(String username) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/users/" + username)
            .build(),
        101);
  }

  @Override
  protected void prepareTestCreateGroup_OK(String groupname) throws Exception {
    respondSuccess(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/" + groupname)
            .build());
  }

  @Override
  protected void prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists(String groupname) throws Exception {
    respondFailure(RestRequest.builder().method(POST).url("/cloud/groups/" + groupname).build(), 102);
  }

  @Override
  protected void prepareTestDeleteGroup_OK(String groupname) throws Exception {
    respondSuccess(RestRequest.builder().method(DELETE).url("/cloud/groups/" + groupname).build());
  }

  @Override
  protected void prepareTestDeleteGroup_NOK_GroupNotFound(String groupname) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/" + groupname)
            .build(),
        101);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_IllegalArgument() throws Exception {
    OwncloudModificationUser user = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .newUser(user)
            .errorCodeCreateUser(101)
            .build());

    userModificationService.saveUser(user);
  }

  @Test(expected = OwncloudUsernameAlreadyExistsException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_OwncloudUsernameAlreadyExistsException() throws Exception {
    OwncloudModificationUser user = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .newUser(user)
            .errorCodeCreateUser(102)
            .build());

    userModificationService.saveUser(user);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_IllegalStateException() throws Exception {
    OwncloudModificationUser user = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .newUser(user)
            .errorCodeCreateUser(103)
            .build());

    userModificationService.saveUser(user);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_AccessDeniedException() throws Exception {
    OwncloudModificationUser user = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .newUser(user)
            .errorCodeCreateUser(997)
            .build());

    userModificationService.saveUser(user);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_UnknownError() throws Exception {
    OwncloudModificationUser user = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .newUser(user)
            .errorCodeCreateUser(999)
            .build());

    userModificationService.saveUser(user);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateDisplayName_IllegalStateException_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateDisplayName(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateDisplayName_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateDisplayName(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateDisplayName_AccessDenied() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateDisplayName(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateDisplayName_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateDisplayName(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateEmail_IllegalStateException_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateEmail(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateEmail_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateEmail(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateEmail_AccessDenied() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateEmail(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_UpdateEmail_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeUpdateEmail(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Enable_IllegalStateException_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Enable_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Enable_AccessDenied() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Enable_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Disable_IllegalStateExcreption_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Disable_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Disable_AccessDenied() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_Disable_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeEnableDisable(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_IllegalArgumentException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_OwncloudGroupNotFoundException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_IllegalStateException_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(103)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_AccessDeniedException_NotAllowedToAddGroup() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(104)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(105)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_AccessDeniedException_NoPermissions() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_AddGroup_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .group("group2")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeAddGroup(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_IllegalArgumentException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(101)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_OwncloudGroupNotFoundException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(102)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_IllegalStateException_UsernameNotFound() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(103)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_AccessDeniedException_NotAllowedToRemoveGroup() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(104)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_IllegalStateException() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(105)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_AccessDeniedException_NoPermissions() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(997)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_NOK_RemoveGroup_UnknownError() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 5")
        .email("user5@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(false)
        .displayname("changed Value")
        .email("changed Value")
        .group("group1")
        .build();

    prepareModificationRestTest(
        UserModification.builder()
            .existingUser(existingUser)
            .newUser(updateUser)
            .errorCodeRemoveGroup(999)
            .build());

    userModificationService.saveUser(updateUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testDeleteUser_NOK_AccessDeniedException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/users/user1")
            .build(),
        997);
    userModificationService.deleteUser("user1");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testDeleteUser_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/users/user1")
            .build(),
        999);
    userModificationService.deleteUser("user1");
  }

  @Test(expected = IllegalArgumentException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_IllegalArgumentException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        101);
    userModificationService.createGroup("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_CannotCreateGroup() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        103);
    userModificationService.createGroup("group4");
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_AccessDeniedException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        997);
    userModificationService.createGroup("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        999);
    userModificationService.createGroup("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_CannotDeleteGroup() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        102);
    userModificationService.deleteGroup("group4");
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_AccessDeniedException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        997);
    userModificationService.deleteGroup("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        999);
    userModificationService.deleteGroup("group4");
  }

  @Data
  @Builder
  private static class UserModification {

    private final OwncloudModificationUser existingUser;
    private final OwncloudModificationUser newUser;
    private int errorCodeCreateUser = 0;
    private int errorCodeUpdateDisplayName = 0;
    private int errorCodeUpdateEmail = 0;
    private int errorCodeEnableDisable = 0;
    private int errorCodeAddGroup = 0;
    private int errorCodeRemoveGroup = 0;

    private static class UserModificationBuilder {
      private int errorCodeCreateUser = 0;
      private int errorCodeUpdateDisplayName = 0;
      private int errorCodeUpdateEmail = 0;
      private int errorCodeEnableDisable = 0;
      private int errorCodeAddGroup = 0;
      private int errorCodeRemoveGroup = 0;
    }

    public boolean isErrorCreateUser() {
      return errorCodeCreateUser != 0;
    }

    public boolean isErrorUpdateDisplayName() {
      return errorCodeUpdateDisplayName != 0;
    }

    public boolean isErrorUpdateEmail() {
      return errorCodeUpdateEmail != 0;
    }

    public boolean isErrorEnableDisable() {
      return errorCodeEnableDisable != 0;
    }

    public boolean isErrorAddGroup() {
      return errorCodeAddGroup != 0;
    }

    public boolean isErrorRemoveGroup() {
      return errorCodeRemoveGroup != 0;
    }
  }

}
