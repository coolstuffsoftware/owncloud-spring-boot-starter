package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserDetailsServiceRestTest;

@ActiveProfiles("URL-TEST-ADMINISTRATOR")
public class OwncloudUserDetailsServiceRestAdministratorTest extends AbstractOwncloudUserDetailsServiceRestTest {

  @Override
  protected String getBasicAuthorizationHeader() {
    return getDefaultBasicAuthorizationHeader();
  }

}
