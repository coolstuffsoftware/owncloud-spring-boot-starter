package software.coolstuff.springframework.owncloud.service;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
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
        .displayName("Mr. User 1")
        .email("user1@example.com")
        .authorities(Lists.newArrayList(new SimpleGrantedAuthority("group1"), new SimpleGrantedAuthority("group2")))
        .build();

    OwncloudModificationUser modificationUser = new OwncloudModificationUser(userDetails);
    verifyServer();

    Assert.assertNotNull(modificationUser);
    Assert.assertEquals(userDetails.getUsername(), modificationUser.getUsername());
    Assert.assertEquals(userDetails.getPassword(), modificationUser.getPassword());
    Assert.assertEquals(userDetails.isEnabled(), modificationUser.isEnabled());
    Assert.assertEquals(userDetails.getDisplayName(), modificationUser.getDisplayName());
    Assert.assertEquals(userDetails.getEmail(), modificationUser.getEmail());
    Assert.assertTrue(CollectionUtils.isEqualCollection(modificationUser.getGroups(), Lists.newArrayList("group1", "group2")));
  }

  @Test
  public void testOwncloudModificationUserBuilderWithoutGroups() throws Exception {
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username("user1")
        .password("password")
        .enabled(true)
        .displayName("Mr. User 1")
        .email("user1@example.com")
        .build();

    OwncloudModificationUser modificationUser = new OwncloudModificationUser(userDetails);
    verifyServer();

    Assert.assertNotNull(modificationUser);
    Assert.assertEquals(userDetails.getUsername(), modificationUser.getUsername());
    Assert.assertEquals(userDetails.getPassword(), modificationUser.getPassword());
    Assert.assertEquals(userDetails.isEnabled(), modificationUser.isEnabled());
    Assert.assertEquals(userDetails.getDisplayName(), modificationUser.getDisplayName());
    Assert.assertEquals(userDetails.getEmail(), modificationUser.getEmail());
    Assert.assertTrue(CollectionUtils.isEmpty(modificationUser.getGroups()));
  }

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_OK_WithoutGroups() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user3")
        .password("password")
        .enabled(true)
        .displayName("Mr. User 3")
        .email("user3@example.com")
        .build();

    prepareTestSaveUser_CreateUser_OK_WithoutGroups(newUser);

    OwncloudUserDetails createdUser = userModificationService.saveUser(newUser);
    verifyServer();

    Assert.assertNotNull(createdUser);
    Assert.assertEquals(newUser.getUsername(), createdUser.getUsername());
    Assert.assertEquals(newUser.getPassword(), createdUser.getPassword());
    Assert.assertEquals(newUser.isEnabled(), createdUser.isEnabled());
    Assert.assertEquals(newUser.getDisplayName(), createdUser.getDisplayName());
    Assert.assertEquals(newUser.getEmail(), createdUser.getEmail());

    checkAuthorities(createdUser.getAuthorities());
  }

  protected void prepareTestSaveUser_CreateUser_OK_WithoutGroups(OwncloudModificationUser newUser) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_OK_WithGroups() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user4")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 4")
        .email("user4@example.com")
        .groups(Lists.newArrayList("group1", "group2"))
        .build();

    prepareTestSaveUser_CreateUser_OK_WithGroups(newUser);

    OwncloudUserDetails createdUser = userModificationService.saveUser(newUser);
    verifyServer();

    Assert.assertNotNull(createdUser);
    Assert.assertEquals(newUser.getUsername(), createdUser.getUsername());
    Assert.assertEquals(newUser.getPassword(), createdUser.getPassword());
    Assert.assertEquals(newUser.isEnabled(), createdUser.isEnabled());
    Assert.assertEquals(newUser.getDisplayName(), createdUser.getDisplayName());
    Assert.assertEquals(newUser.getEmail(), createdUser.getEmail());

    checkAuthorities(createdUser.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestSaveUser_CreateUser_OK_WithGroups(OwncloudModificationUser newUser) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_UpdateUser_OK_WithoutGroups() throws Exception {
    OwncloudModificationUser existingUser = OwncloudModificationUser.builder()
        .username("user2")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 1")
        .email("user2@example.com")
        .build();

    OwncloudModificationUser updateUser = OwncloudModificationUser.builder()
        .username("user2")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 2 in Subdomain")
        .email("user2@subdomain.example.com")
        .build();

    prepareTestSaveUser_UpdateUser_OK_WithoutGroups(existingUser, updateUser);

    OwncloudUserDetails updatedUser = userModificationService.saveUser(updateUser);
    verifyServer();

    Assert.assertNotNull(updatedUser);
    Assert.assertEquals(updateUser.getUsername(), updatedUser.getUsername());
    Assert.assertEquals(updateUser.getPassword(), updatedUser.getPassword());
    Assert.assertEquals(updateUser.isEnabled(), updatedUser.isEnabled());
    Assert.assertEquals(updateUser.getDisplayName(), updatedUser.getDisplayName());
    Assert.assertEquals(updateUser.getEmail(), updatedUser.getEmail());
    Assert.assertTrue(CollectionUtils.isEmpty(updatedUser.getAuthorities()));
  }

  protected void prepareTestSaveUser_UpdateUser_OK_WithoutGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {}
}
