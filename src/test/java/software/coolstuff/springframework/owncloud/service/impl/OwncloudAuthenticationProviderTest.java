package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.client.MockRestServiceServer;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

@RestClientTest(OwncloudAuthenticationProvider.class)
public class OwncloudAuthenticationProviderTest extends AbstractOwncloudTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  private MockRestServiceServer server;

  @Override
  protected String getResourcePrefix() {
    return "/authentication";
  }

  @Before
  public void setUp() {
    server = MockRestServiceServer.createServer(((OwncloudAuthenticationProvider) authenticationProvider).getRestTemplate());
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
  public void testAuthenticate_OK() throws IOException {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("password")
        .build();

    server
        .expect(requestToWithPrefix("/users/" + credentials.getUsername()))
        .andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_details"), MediaType.TEXT_XML));
    server
        .expect(requestToWithPrefix("/users/" + credentials.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_groups"), MediaType.TEXT_XML));

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
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

  @Test(expected = BadCredentialsException.class)
  public void testAuthenticate_NOK() {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("wrongPassword")
        .build();

    server
        .expect(requestToWithPrefix("/users/user1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withUnauthorizedRequest());

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
    authenticationProvider.authenticate(authenticationToken);
  }
}
