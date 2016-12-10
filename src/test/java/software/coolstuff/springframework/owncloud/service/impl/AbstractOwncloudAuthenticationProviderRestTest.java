package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpStatusCodeException;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudAuthenticationProviderTest;

public abstract class AbstractOwncloudAuthenticationProviderRestTest extends AbstractOwncloudAuthenticationProviderTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudAuthenticationProvider authenticationProvider;

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public final OwncloudAuthenticationProvider owncloudService() {
    return authenticationProvider;
  }

  @Override
  protected void prepareTestAuthenticate_OK(Credentials credentials, boolean enabled, String email, String displayName, String... groups) throws IOException {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + credentials.getUsername())
            .basicAuthentication(credentials.getBasicAuthorizationHeader())
            .build(),
        enabled, email, displayName);
    respondGroups(
        RestRequest.builder()
            .server(createServer(userDetailsService))
            .method(GET)
            .url("/cloud/users/" + credentials.getUsername() + "/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws MalformedURLException {
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + credentials.getUsername())
            .basicAuthentication(credentials.getBasicAuthorizationHeader())
            .build(),
        HttpStatus.UNAUTHORIZED);
  }

  @Test(expected = HttpStatusCodeException.class)
  public void test404NotFound() throws Exception {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("password")
        .build();
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + credentials.getUsername())
            .basicAuthentication(credentials.getBasicAuthorizationHeader())
            .build(),
        HttpStatus.NOT_FOUND);
    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  @Test(expected = BadCredentialsException.class)
  public void testBadCredentialsByRestError() throws Exception {
    Credentials credentials = Credentials.builder()
        .username("user1")
        .password("password")
        .build();
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + credentials.getUsername())
            .basicAuthentication(credentials.getBasicAuthorizationHeader())
            .build(),
        999);
    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

}
