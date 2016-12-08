package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserModificationServiceRestTest;

@ActiveProfiles("URL-TEST-AUTHENTICATED-USER")
public class OwncloudUserModificationServiceRestAuthenticatedUserTest extends AbstractOwncloudUserModificationServiceRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

}
