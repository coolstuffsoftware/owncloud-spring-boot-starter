package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;

public abstract class AbstractOwncloudUserDetailsServiceRestTest extends AbstractOwncloudUserDetailsServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public final OwncloudUserDetailsService owncloudService() {
    return userDetailsService;
  }

  @Override
  protected void prepareTestUserDetails_OK(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_details"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_groups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestUserDetails_NotFound() throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/unknown"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("unknown_user"), MediaType.TEXT_XML));
  }

}
