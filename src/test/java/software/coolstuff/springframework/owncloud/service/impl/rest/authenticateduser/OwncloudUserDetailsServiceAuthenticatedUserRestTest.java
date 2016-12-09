package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import static org.springframework.http.HttpMethod.GET;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserDetailsServiceRestTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@ActiveProfiles("AUTHENTICATED-USER-URL")
public class OwncloudUserDetailsServiceAuthenticatedUserRestTest extends AbstractOwncloudUserDetailsServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public final String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Test(expected = OwncloudInvalidAuthenticationObjectException.class)
  @WithAnonymousUser
  public void testUserDetails_WrongAuthenticationObject() throws MalformedURLException, IOException {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/user/user1")
            .build(),
        true,
        "user1@example.com",
        "Mr. User 1");

    userDetailsService.loadUserByUsername("user1");
  }

}
