package software.coolstuff.springframework.owncloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserModificationService.class)
public abstract class AbstractOwncloudUserModificationServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  protected final String getResourcePrefix() {
    return "/modificationService";
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
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
  @WithMockOwncloudUser(username = "user1", password = "password")
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
}
