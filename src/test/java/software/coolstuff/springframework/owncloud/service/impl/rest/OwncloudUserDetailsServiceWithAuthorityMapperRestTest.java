package software.coolstuff.springframework.owncloud.service.impl.rest;

import static org.springframework.http.HttpMethod.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceWithAuthorityMapperTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudServiceRestTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@ActiveProfiles("AUTHORITY-MAPPER-URL")
public class OwncloudUserDetailsServiceWithAuthorityMapperRestTest extends AbstractOwncloudUserDetailsServiceWithAuthorityMapperTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public OwncloudUserDetailsService owncloudService() {
    return userDetailsService;
  }

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Override
  protected void prepareTestMappedGroups(String username, boolean enabled, String email, String displayName, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + username)
            .build(),
        enabled,
        email,
        displayName);
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + username + "/groups")
            .build(),
        groups);
  }
}
