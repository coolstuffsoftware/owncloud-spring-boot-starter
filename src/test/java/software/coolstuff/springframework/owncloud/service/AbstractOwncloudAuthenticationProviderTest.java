package software.coolstuff.springframework.owncloud.service;

import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;

@RestClientTest(OwncloudAuthenticationProvider.class)
public abstract class AbstractOwncloudAuthenticationProviderTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudAuthenticationProvider authenticationProvider;

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
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void testAuthenticate_OK() throws Exception {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("s3cr3t")
        .build();

    prepareTestAuthenticate_OK(credentials, true, "user1@example.com", "Mr. User 1", "group1", "group2");

    Authentication authentication = authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
    verifyServer();

    Assert.assertNotNull(authentication);
    Assert.assertTrue(OwncloudAuthentication.class.isAssignableFrom(authentication.getClass()));
    checkAuthorities(authentication.getAuthorities(), "group1", "group2");

    Assert.assertEquals(credentials.getUsername(), authentication.getName());
    Assert.assertEquals(credentials.getPassword(), authentication.getCredentials());

    Assert.assertNotNull(authentication.getPrincipal());
    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(authentication.getPrincipal().getClass()));
    OwncloudUserDetails principal = (OwncloudUserDetails) authentication.getPrincipal();
    Assert.assertTrue(principal.isEnabled());
    Assert.assertEquals("Mr. User 1", principal.getDisplayName());
    Assert.assertEquals("user1@example.com", principal.getEmail());
    Assert.assertEquals(2, principal.getAuthorities().size());
  }

  protected void prepareTestAuthenticate_OK(Credentials credentials, boolean enabled, String email, String displayName, String... groups) throws Exception {};

  @Test(expected = BadCredentialsException.class)
  public void testAuthenticate_NOK() throws Exception {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("wrongPassword")
        .build();

    prepareTestAuthenticate_NOK(credentials);

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws Exception {};

  @Test(expected = BadCredentialsException.class)
  public void testAuthentication_NOK_NoUser() throws Exception {
    Credentials credentials = Credentials.builder()
        .username(null)
        .password(null)
        .build();

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  @Test(expected = BadCredentialsException.class)
  public void testAuthentication_NOK_NoPassword() throws Exception {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password(null)
        .build();

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  @Data
  @Builder
  protected static class Credentials {

    private final String username;
    private final String password;

    public String getBasicAuthorizationHeader() {
      return "Basic " + Base64.getEncoder().encodeToString((getUsername() + ":" + getPassword()).getBytes());
    }

    public UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken() {
      return new UsernamePasswordAuthenticationToken(username, password);
    }
  }
}
