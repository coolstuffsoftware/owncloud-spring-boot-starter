package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudAuthenticationProviderRestTest;

@ActiveProfiles("URL-TEST-ADMINISTRATOR")
public class OwncloudAuthenticationProviderAdministratorRestTest extends AbstractOwncloudAuthenticationProviderRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getDefaultBasicAuthorizationHeader();
  }

}
