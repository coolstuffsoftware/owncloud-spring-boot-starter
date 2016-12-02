package software.coolstuff.springframework.owncloud.service.impl.rest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudRestTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudAuthenticationProvider.class)
public abstract class AbstractOwncloudAuthenticationProviderRestTest extends AbstractOwncloudRestTest {

  @Autowired
  private OwncloudAuthenticationProvider authenticationProvider;

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  protected final String getResourcePrefix() {
    return "/authentication";
  }

  @Override
  protected final OwncloudAuthenticationProvider owncloudService() {
    return authenticationProvider;
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
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testAuthenticate_OK() throws IOException {
    Credentials credentials = Credentials.builder().username("user1").password("password").build();

    server.expect(requestToWithPrefix("/cloud/users/" + credentials.getUsername())).andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_details"), MediaType.TEXT_XML));
    createServer(userDetailsService)
        .expect(requestToWithPrefix("/cloud/users/" + credentials.getUsername() + "/groups")).andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthenticationHeaderForUserDetailsService()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_groups"), MediaType.TEXT_XML));

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        credentials.getUsername(), credentials.getPassword());
    Authentication authentication = authenticationProvider.authenticate(authenticationToken);
    server.verify();

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

  protected abstract String getBasicAuthenticationHeaderForUserDetailsService();

  @Test(expected = BadCredentialsException.class)
  public void testAuthenticate_NOK() throws MalformedURLException {
    Credentials credentials = Credentials.builder().username("user1").password("wrongPassword").build();

    server.expect(requestToWithPrefix("/cloud/users/user1")).andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withUnauthorizedRequest());

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        credentials.getUsername(), credentials.getPassword());
    authenticationProvider.authenticate(authenticationToken);
  }
}
