package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserQueryServiceRestTest;

@ActiveProfiles("AUTHENTICATED-USER-URL")
public class OwncloudUserQueryServiceAuthenticatedUserRestTest extends AbstractOwncloudUserQueryServiceRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

}
