package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceWithAuthorityAppenderTest;

@ActiveProfiles("AUTHORITY-APPENDER-URL")
public class OwncloudUserDetailsServiceWithAuthorityAppenderRestTest extends AbstractOwncloudUserDetailsServiceWithAuthorityAppenderTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public AbstractOwncloudServiceImpl owncloudService() {
    return userDetailsService;
  }

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Override
  protected void prepareTestAppendedGroups(String username) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + username))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_details"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + username + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_groups"), MediaType.TEXT_XML));
  }
}
