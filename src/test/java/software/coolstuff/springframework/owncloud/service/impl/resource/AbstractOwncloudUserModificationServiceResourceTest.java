package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudResourceTest;

@RestClientTest(OwncloudUserModificationService.class)
public abstract class AbstractOwncloudUserModificationServiceResourceTest extends AbstractOwncloudResourceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Override
  protected final String getResourcePrefix() {
    return "/modificationService";
  }

  @Test
  public void testSaveUser_CreateUser_OK_WithoutGroups() {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user1")
        .password("password")
        .enabled(true)
        .displayName("Mr. User 1")
        .email("user1@example.com")
        .build();

    OwncloudUserDetails createdUser = userModificationService.saveUser(newUser);
    Assert.assertNotNull(createdUser);
    Assert.assertEquals(newUser.getUsername(), createdUser.getUsername());
    Assert.assertEquals(newUser.getPassword(), createdUser.getPassword());
    Assert.assertEquals(newUser.isEnabled(), createdUser.isEnabled());
    Assert.assertEquals(newUser.getDisplayName(), createdUser.getDisplayName());
    Assert.assertEquals(newUser.getEmail(), createdUser.getEmail());

    checkAuthorities(createdUser.getAuthorities());
  }

}
