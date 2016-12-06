package software.coolstuff.springframework.owncloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;

@RestClientTest(OwncloudAuthenticationProvider.class)
public abstract class AbstractOwncloudAuthenticationProviderTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudAuthenticationProvider authenticationProvider;

  @Override
  protected final String getResourcePrefix() {
    return "/authentication";
  }

  @Test
  public void testCorrectClass() {
    Assert.assertEquals(OwncloudAuthenticationProvider.class, authenticationProvider.getClass());
  }

  @Test
  public void testSupportedAuthenticationTokens() {
    Assert.assertTrue(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    Assert.assertTrue(authenticationProvider.supports(OwncloudAuthentication.class));
    Assert.assertFalse(authenticationProvider.supports(RememberMeAuthenticationToken.class));
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "s3cr3t")
  public void testAuthenticate_OK() throws Exception {
    Credentials credentials = Credentials.builder().username("user1").password("s3cr3t").build();

    prepareTestAuthenticate_OK(credentials);

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        credentials.getUsername(), credentials.getPassword());
    Authentication authentication = authenticationProvider.authenticate(authenticationToken);
    verifyServer();

    Assert.assertNotNull(authentication);
    Assert.assertTrue(OwncloudAuthentication.class.isAssignableFrom(authentication.getClass()));

    Assert.assertEquals(credentials.getUsername(), authentication.getName());
    Assert.assertEquals(credentials.getPassword(), authentication.getCredentials());

    Assert.assertNotNull(authentication.getPrincipal());
    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(authentication.getPrincipal().getClass()));
    OwncloudUserDetails principal = (OwncloudUserDetails) authentication.getPrincipal();
    Assert.assertTrue(principal.isEnabled());
    Assert.assertEquals("Mr. User 1", principal.getDisplayName());
    Assert.assertEquals("user1@example.com", principal.getEmail());
    Assert.assertEquals(2, principal.getAuthorities().size());

    checkAuthorities(principal.getAuthorities(), "Group1", "Group2");
  }

  protected void prepareTestAuthenticate_OK(Credentials credentials) throws Exception {};

  @Test(expected = BadCredentialsException.class)
  public void testAuthenticate_NOK() throws Exception {
    Credentials credentials = Credentials.builder().username("user1").password("wrongPassword").build();

    prepareTestAuthenticate_NOK(credentials);

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        credentials.getUsername(), credentials.getPassword());
    authenticationProvider.authenticate(authenticationToken);
  }

  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws Exception {};
}
