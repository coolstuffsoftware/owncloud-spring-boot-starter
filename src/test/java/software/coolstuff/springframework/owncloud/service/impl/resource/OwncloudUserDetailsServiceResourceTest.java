package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.AbstractOwncloudTest;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudUserDetailsService.class)
@ActiveProfiles("RESOURCE-TEST")
public class OwncloudUserDetailsServiceResourceTest extends AbstractOwncloudTest {

  @Autowired
  private UserDetailsService userDetailsService;

  @Override
  protected String getResourcePrefix() {
    return "/userDetails";
  }

  @Test
  public void testOK() {
    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");

    Assert.assertNotNull(userDetails);

    Assert.assertEquals("user1", userDetails.getUsername());
    Assert.assertNull(userDetails.getPassword());
    checkAuthorities(userDetails.getAuthorities(), "Group1", "Group2");

    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(userDetails.getClass()));
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetails;
    Assert.assertEquals("Mr. User 1", owncloudUserDetails.getDisplayName());
    Assert.assertEquals("user1@example.com", owncloudUserDetails.getEmail());
  }

  @Test(expected = UsernameNotFoundException.class)
  public void testNOK() {
    userDetailsService.loadUserByUsername("unknown");
  }
}
