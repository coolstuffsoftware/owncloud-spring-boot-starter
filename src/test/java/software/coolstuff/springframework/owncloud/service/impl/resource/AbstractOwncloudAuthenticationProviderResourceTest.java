package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudResourceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;

@RestClientTest(OwncloudAuthenticationProvider.class)
public abstract class AbstractOwncloudAuthenticationProviderResourceTest extends AbstractOwncloudResourceTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Override
  protected final String getResourcePrefix() {
    return "/authentication";
  }

  @Test
  public void testOK() {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken("user1", "s3cr3t");
    Authentication authentication = authenticationProvider.authenticate(authenticationToken);

    Assert.assertNotNull(authentication);
    Assert.assertTrue(OwncloudAuthentication.class.isAssignableFrom(authentication.getClass()));

    Assert.assertEquals("user1", authentication.getName());
    Assert.assertEquals("s3cr3t", authentication.getCredentials());

    Assert.assertNotNull(authentication.getPrincipal());
    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(authentication.getPrincipal().getClass()));
    OwncloudUserDetails principal = (OwncloudUserDetails) authentication.getPrincipal();
    Assert.assertTrue(principal.isEnabled());
    Assert.assertEquals("Mr. User 1", principal.getDisplayName());
    Assert.assertEquals("user1@example.com", principal.getEmail());
    Assert.assertEquals(2, principal.getAuthorities().size());

    checkAuthorities(principal.getAuthorities(), "Group1", "Group2");
  }

  @CompareResourceAfter("testOK")
  public void compareAfterTestOK(Resource target) throws Exception {
    compareResources(target);
  }

  @Test(expected = BadCredentialsException.class)
  public void testNOK() {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken("user1", "wrongPassword");
    authenticationProvider.authenticate(authenticationToken);
  }

  @CompareResourceAfter("testNOK")
  public void compareAfterTestNOK(Resource target) throws Exception {
    compareResources(target);
  }
}
