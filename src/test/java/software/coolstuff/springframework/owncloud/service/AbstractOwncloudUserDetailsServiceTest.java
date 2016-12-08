package software.coolstuff.springframework.owncloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudUserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  protected final String getResourcePrefix() {
    return "/userDetails";
  }

  @Test
  public void testCorrectClass() {
    Assert.assertEquals(OwncloudUserDetailsService.class, userDetailsService.getClass());
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testUserDetails_OK() throws Exception {
    prepareTestUserDetails_OK("user1");

    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");
    verifyServer();

    Assert.assertNotNull(userDetails);

    Assert.assertEquals("user1", userDetails.getUsername());
    Assert.assertNull(userDetails.getPassword());
    checkAuthorities(userDetails.getAuthorities(), "group1", "group2");

    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(userDetails.getClass()));
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetails;
    Assert.assertEquals("Mr. User 1", owncloudUserDetails.getDisplayName());
    Assert.assertEquals("user1@example.com", owncloudUserDetails.getEmail());
  }

  protected void prepareTestUserDetails_OK(String user) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testUserDetails_NotFound() throws Exception {
    prepareTestUserDetails_NotFound();

    userDetailsService.loadUserByUsername("unknown");
  }

  protected void prepareTestUserDetails_NotFound() throws Exception {}

}
