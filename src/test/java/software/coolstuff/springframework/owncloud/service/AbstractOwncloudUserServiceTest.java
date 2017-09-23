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
package software.coolstuff.springframework.owncloud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserService.class)
public abstract class AbstractOwncloudUserServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserService userService;

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllUsers() throws Exception {
    prepareTestFindAllUsers("user1", "user2");

    List<String> users = userService.findAll();
    verifyServer();

    assertThat(users)
        .isNotNull()
        .containsOnly("user1", "user2");
  }

  protected void prepareTestFindAllUsers(String... users) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllUsersWithFilter() throws Exception {
    prepareTestFindAllUsersWithFilter("User 1", "user1");

    List<String> users = userService.findAll("User 1");
    verifyServer();

    assertThat(users)
        .isNotNull()
        .containsOnly("user1");
  }

  protected void prepareTestFindAllUsersWithFilter(String filter, String... users) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_OK() throws Exception {
    OwncloudUserDetails expectedUser = OwncloudUserDetails.builder()
        .username("user1")
        .enabled(true)
        .email("user1@example.com")
        .displayname("Mr. User 1")
        .quota(1024L)
        .build();
    prepareTestFindOneUser_OK(expectedUser, "group1", "group2");

    OwncloudUserDetails actualUser = userService.findOne("user1");
    verifyServer();

    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getUsername()).isEqualTo(expectedUser.getUsername());
    assertThat(actualUser.getPassword()).isNull();
    assertThat(actualUser.getDisplayname()).isEqualTo(expectedUser.getDisplayname());
    assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
    assertThat(actualUser.getQuota()).isEqualByComparingTo(expectedUser.getQuota());

    checkAuthorities(actualUser.getUsername(), actualUser.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockUser(username = "user3", password = "password")
  public void testFindOneUser_UnknownUser() throws Exception {
    prepareTestFindOneUser_UnknownUser("user3");
    userService.findOne("user3");
  }

  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {}

  @Test(expected = NullPointerException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_NoUser() {
    userService.findOne(null);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_BlankUser() {
    userService.findOne("");
  }

  @Test
  public void testOwncloudModificationUserBuilderWithGroups() throws Exception {
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username("user1")
        .password("password")
        .enabled(true)
        .displayname("Mr. User 1")
        .email("user1@example.com")
        .authorities(Lists.newArrayList(
            new SimpleGrantedAuthority("group1"),
            new SimpleGrantedAuthority("group2")))
        .build();

    OwncloudModificationUser modificationUser = new OwncloudModificationUser(userDetails);
    verifyServer();

    assertThat(modificationUser).isNotNull();
    assertThat(modificationUser.getUsername()).isEqualTo(userDetails.getUsername());
    assertThat(modificationUser.getPassword()).isEqualTo(userDetails.getPassword());
    assertThat(modificationUser.isEnabled()).isEqualTo(userDetails.isEnabled());
    assertThat(modificationUser.getDisplayname()).isEqualTo(userDetails.getDisplayname());
    assertThat(modificationUser.getEmail()).isEqualTo(userDetails.getEmail());
    assertThat(modificationUser.getGroups()).containsOnly("group1", "group2");
  }

  @Test
  public void testOwncloudModificationUserBuilderWithoutGroups() throws Exception {
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username("user1")
        .password("password")
        .enabled(true)
        .displayname("Mr. User 1")
        .email("user1@example.com")
        .build();

    OwncloudModificationUser modificationUser = new OwncloudModificationUser(userDetails);
    verifyServer();

    assertThat(modificationUser).isNotNull();
    assertThat(modificationUser.getUsername()).isEqualTo(userDetails.getUsername());
    assertThat(modificationUser.getPassword()).isEqualTo(userDetails.getPassword());
    assertThat(modificationUser.isEnabled()).isEqualTo(userDetails.isEnabled());
    assertThat(modificationUser.getDisplayname()).isEqualTo(userDetails.getDisplayname());
    assertThat(modificationUser.getEmail()).isEqualTo(userDetails.getEmail());
    assertThat(modificationUser.getGroups()).isEmpty();
  }

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_OK_WithoutGroups() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user3")
        .password("password")
        .enabled(true)
        .displayname("Mr. User 3")
        .email("user3@example.com")
        .quota(1024L)
        .build();

    prepareTestSaveUser_CreateUser_OK_WithoutGroups(newUser);

    OwncloudUserDetails createdUser = userService.save(newUser);
    verifyServer();

    assertThat(createdUser).isNotNull();
    assertThat(createdUser.getUsername()).isEqualTo(newUser.getUsername());
    assertThat(createdUser.getPassword()).isNull();
    assertThat(createdUser.isEnabled()).isEqualTo(newUser.isEnabled());
    assertThat(createdUser.getDisplayname()).isEqualTo(newUser.getDisplayname());
    assertThat(createdUser.getEmail()).isEqualTo(newUser.getEmail());

    checkAuthorities(createdUser.getUsername(), createdUser.getAuthorities());
  }

  protected void prepareTestSaveUser_CreateUser_OK_WithoutGroups(OwncloudModificationUser newUser) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_OK_WithGroups() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user4")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 4")
        .email("user4@example.com")
        .quota(1024L)
        .group("group1")
        .group("group2")
        .build();

    prepareTestSaveUser_CreateUser_OK_WithGroups(newUser);

    OwncloudUserDetails createdUser = userService.save(newUser);
    verifyServer();

    assertThat(createdUser).isNotNull();
    assertThat(createdUser.getUsername()).isEqualTo(newUser.getUsername());
    assertThat(createdUser.getPassword()).isNull();
    assertThat(createdUser.isEnabled()).isEqualTo(newUser.isEnabled());
    assertThat(createdUser.getDisplayname()).isEqualTo(newUser.getDisplayname());
    assertThat(createdUser.getEmail()).isEqualTo(newUser.getEmail());

    checkAuthorities(createdUser.getUsername(), createdUser.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestSaveUser_CreateUser_OK_WithGroups(OwncloudModificationUser newUser) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_OK_WithoutGroups() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user2")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 1")
        .email("user2@example.com")
        .quota(1024L)
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user2")
        .password("password")
        .enabled(true)
        .displayname("Mrs. User 2 in Subdomain")
        .email("user2@subdomain.example.com")
        .quota(2048L)
        .build();

    prepareTestSaveUser_UpdateUser_OK_WithoutGroups(existingUser, updateUser);

    OwncloudUserDetails updatedUser = userService.save(updateUser);
    verifyServer();

    assertThat(updatedUser).isNotNull();
    assertThat(updatedUser.getUsername()).isEqualTo(updateUser.getUsername());
    assertThat(updatedUser.getPassword()).isNull();
    assertThat(updatedUser.isEnabled()).isEqualTo(updateUser.isEnabled());
    assertThat(updatedUser.getDisplayname()).isEqualTo(updateUser.getDisplayname());
    assertThat(updatedUser.getEmail()).isEqualTo(updateUser.getEmail());
    assertThat(updatedUser.getQuota()).isEqualTo(updateUser.getQuota());
    assertThat(updatedUser.getAuthorities()).isEmpty();
  }

  protected void prepareTestSaveUser_UpdateUser_OK_WithoutGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_OK_WithGroups() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user1")
        .password("s3cr3t")
        .enabled(true)
        .displayname("Mr. User 1")
        .email("user1@example.com")
        .group("group1")
        .group("group2")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user1")
        .password("s3cr3t")
        .enabled(false) // disabled instead of enabled
        .displayname("Mr. User 1")
        .email("user1@example.com")
        .group("group1")
        .group("group3") // group3 instead of group2
        .build();

    prepareTestSaveUser_UpdateUser_OK_WithGroups(existingUser, updateUser);

    OwncloudUserDetails updatedUser = userService.save(updateUser);
    verifyServer();

    assertThat(updatedUser).isNotNull();
    assertThat(updatedUser.getUsername()).isEqualTo(updateUser.getUsername());
    assertThat(updatedUser.getPassword()).isNull();
    assertThat(updatedUser.isEnabled()).isEqualTo(updateUser.isEnabled());
    assertThat(updatedUser.getDisplayname()).isEqualTo(updateUser.getDisplayname());
    assertThat(updatedUser.getEmail()).isEqualTo(updateUser.getEmail());
    assertThat(updatedUser.getQuota()).isEqualTo(updateUser.getQuota());

    checkAuthorities(updatedUser.getUsername(), updatedUser.getAuthorities(), updateUser.getGroups().toArray(new String[] {}));
  }

  protected void prepareTestSaveUser_UpdateUser_OK_WithGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteUser_OK() throws Exception {
    prepareTestDeleteUser_OK("user1");
    userService.delete("user1");
  }

  protected void prepareTestDeleteUser_OK(String username) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteUser_NOK_UsernameNotFoundException() throws Exception {
    prepareTestDeleteUser_NOK_UsernameNotFoundException("user3");
    userService.delete("user3");
  }

  protected void prepareTestDeleteUser_NOK_UsernameNotFoundException(String username) throws Exception {}

}
