package software.coolstuff.springframework.owncloud.service.impl.rest.authenticateduser;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudAuthenticationProviderRestTest;

@ActiveProfiles("AUTHENTICATED-USER-URL")
public class OwncloudAuthenticationProviderAuthenticatedUserRestTest extends AbstractOwncloudAuthenticationProviderRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }
}
