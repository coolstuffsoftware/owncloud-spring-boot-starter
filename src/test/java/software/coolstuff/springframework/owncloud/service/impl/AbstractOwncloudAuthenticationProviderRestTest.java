package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

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
  protected void prepareTestAuthenticate_OK(Credentials credentials) throws IOException {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + credentials.getUsername()))
        .andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_details"), MediaType.TEXT_XML));
    createServer(userDetailsService)
        .expect(requestToWithPrefix("/cloud/users/" + credentials.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf(credentials.getUsername() + "_groups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws MalformedURLException {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + credentials.getUsername()))
        .andExpect(method(GET))
        .andExpect(header("Authorization", credentials.getForBasicAuthorizationHeader()))
        .andRespond(withUnauthorizedRequest());
  }
}
