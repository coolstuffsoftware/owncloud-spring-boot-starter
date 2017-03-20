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

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserModificationService.class)
public abstract class AbstractOwncloudUserModificationServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

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

    Assert.assertNotNull(modificationUser);
    Assert.assertEquals(userDetails.getUsername(), modificationUser.getUsername());
    Assert.assertEquals(userDetails.getPassword(), modificationUser.getPassword());
    Assert.assertEquals(userDetails.isEnabled(), modificationUser.isEnabled());
    Assert.assertEquals(userDetails.getDisplayname(), modificationUser.getDisplayname());
    Assert.assertEquals(userDetails.getEmail(), modificationUser.getEmail());
    Assert.assertTrue(CollectionUtils.isEqualCollection(modificationUser.getGroups(), Lists.newArrayList("group1", "group2")));
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

    Assert.assertNotNull(modificationUser);
    Assert.assertEquals(userDetails.getUsername(), modificationUser.getUsername());
    Assert.assertEquals(userDetails.getPassword(), modificationUser.getPassword());
    Assert.assertEquals(userDetails.isEnabled(), modificationUser.isEnabled());
    Assert.assertEquals(userDetails.getDisplayname(), modificationUser.getDisplayname());
    Assert.assertEquals(userDetails.getEmail(), modificationUser.getEmail());
    Assert.assertTrue(CollectionUtils.isEmpty(modificationUser.getGroups()));
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

    OwncloudUserDetails createdUser = userModificationService.saveUser(newUser);
    verifyServer();

    Assert.assertNotNull(createdUser);
    Assert.assertEquals(newUser.getUsername(), createdUser.getUsername());
    Assert.assertNull(createdUser.getPassword());
    Assert.assertEquals(newUser.isEnabled(), createdUser.isEnabled());
    Assert.assertEquals(newUser.getDisplayname(), createdUser.getDisplayname());
    Assert.assertEquals(newUser.getEmail(), createdUser.getEmail());

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

    OwncloudUserDetails createdUser = userModificationService.saveUser(newUser);
    verifyServer();

    Assert.assertNotNull(createdUser);
    Assert.assertEquals(newUser.getUsername(), createdUser.getUsername());
    Assert.assertNull(createdUser.getPassword());
    Assert.assertEquals(newUser.isEnabled(), createdUser.isEnabled());
    Assert.assertEquals(newUser.getDisplayname(), createdUser.getDisplayname());
    Assert.assertEquals(newUser.getEmail(), createdUser.getEmail());

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

    OwncloudUserDetails updatedUser = userModificationService.saveUser(updateUser);
    verifyServer();

    Assert.assertNotNull(updatedUser);
    Assert.assertEquals(updateUser.getUsername(), updatedUser.getUsername());
    Assert.assertNull(updatedUser.getPassword());
    Assert.assertEquals(updateUser.isEnabled(), updatedUser.isEnabled());
    Assert.assertEquals(updateUser.getDisplayname(), updatedUser.getDisplayname());
    Assert.assertEquals(updateUser.getEmail(), updatedUser.getEmail());
    Assert.assertEquals(updateUser.getQuota(), updatedUser.getQuota());
    Assert.assertTrue(CollectionUtils.isEmpty(updatedUser.getAuthorities()));
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

    OwncloudUserDetails updatedUser = userModificationService.saveUser(updateUser);
    verifyServer();

    Assert.assertNotNull(updatedUser);
    Assert.assertEquals(updateUser.getUsername(), updatedUser.getUsername());
    Assert.assertNull(updatedUser.getPassword());
    Assert.assertEquals(updateUser.isEnabled(), updatedUser.isEnabled());
    Assert.assertEquals(updateUser.getDisplayname(), updatedUser.getDisplayname());
    Assert.assertEquals(updateUser.getEmail(), updatedUser.getEmail());
    checkAuthorities(updatedUser.getUsername(), updatedUser.getAuthorities(), updateUser.getGroups().toArray(new String[] {}));
  }

  protected void prepareTestSaveUser_UpdateUser_OK_WithGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteUser_OK() throws Exception {
    prepareTestDeleteUser_OK("user1");
    userModificationService.deleteUser("user1");
  }

  protected void prepareTestDeleteUser_OK(String username) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteUser_NOK_UsernameNotFoundException() throws Exception {
    prepareTestDeleteUser_NOK_UsernameNotFoundException("user3");
    userModificationService.deleteUser("user3");
  }

  protected void prepareTestDeleteUser_NOK_UsernameNotFoundException(String username) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_OK() throws Exception {
    prepareTestCreateGroup_OK("group4");
    userModificationService.createGroup("group4");
  }

  protected void prepareTestCreateGroup_OK(String groupname) throws Exception {}

  @Test(expected = OwncloudGroupAlreadyExistsException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_OwncloudGroupAlreadyExists() throws Exception {
    prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists("group1");
    userModificationService.createGroup("group1");
  }

  protected void prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists(String groupname) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_OK() throws Exception {
    prepareTestDeleteGroup_OK("group1");
    userModificationService.deleteGroup("group1");
  }

  protected void prepareTestDeleteGroup_OK(String groupname) throws Exception {}

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_GroupNotFound() throws Exception {
    prepareTestDeleteGroup_NOK_GroupNotFound("group4");
    userModificationService.deleteGroup("group4");
  }

  protected void prepareTestDeleteGroup_NOK_GroupNotFound(String groupname) throws Exception {}
}
