package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;

import org.springframework.beans.factory.annotation.Autowired;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;

public abstract class AbstractOwncloudUserDetailsServiceRestTest extends AbstractOwncloudUserDetailsServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public final OwncloudUserDetailsService owncloudService() {
    return userDetailsService;
  }

  @Override
  protected void prepareTestUserDetails_OK(String user, boolean enabled, String email, String displayName, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user)
            .build(),
        enabled,
        email,
        displayName);
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user + "/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestUserDetails_NotFound() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/unknown")
            .build(),
        998,
        "The requested user could not be found");
  }

}
