package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;
import software.coolstuff.springframework.owncloud.service.impl.rest.AbstractOwncloudUserDetailsServiceRestTest;

@ActiveProfiles("URL-TEST-AUTHENTICATED-USER")
public class OwncloudUserDetailsServiceRestAuthenticatedUserTest extends AbstractOwncloudUserDetailsServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  protected String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Test(expected = OwncloudInvalidAuthenticationObjectException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testUserDetails_WrongAuthenticationObject() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/users/user1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_details"), MediaType.TEXT_XML));

    userDetailsService.loadUserByUsername("user1");
  }

}
