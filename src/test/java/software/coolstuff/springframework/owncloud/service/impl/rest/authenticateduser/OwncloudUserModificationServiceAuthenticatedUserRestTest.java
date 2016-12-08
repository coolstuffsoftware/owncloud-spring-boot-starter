package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserModificationServiceRestTest;

@ActiveProfiles("AUTHENTICATED-USER-URL")
public class OwncloudUserModificationServiceAuthenticatedUserRestTest extends AbstractOwncloudUserModificationServiceRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

}
