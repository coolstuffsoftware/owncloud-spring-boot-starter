package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserQueryServiceRestTest;

@ActiveProfiles("URL-TEST-AUTHENTICATED-USER")
public class OwncloudUserQueryServiceRestAuthenticatedUserTest extends AbstractOwncloudUserQueryServiceRestTest {

  @Override
  protected String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

}
