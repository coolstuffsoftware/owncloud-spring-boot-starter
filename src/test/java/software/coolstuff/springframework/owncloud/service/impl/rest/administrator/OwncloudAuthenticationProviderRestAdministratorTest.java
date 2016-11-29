package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.rest.AbstractOwncloudAuthenticationProviderRestTest;

@ActiveProfiles("URL-TEST-ADMINISTRATOR")
public class OwncloudAuthenticationProviderRestAdministratorTest extends AbstractOwncloudAuthenticationProviderRestTest {

  @Override
  protected String getBasicAuthenticationHeaderForUserDetailsService() {
    return getDefaultBasicAuthorizationHeader();
  }

}
