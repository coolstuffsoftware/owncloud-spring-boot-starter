package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserDetailsServiceRestTest;

@ActiveProfiles("ADMINISTRATOR-URL")
public class OwncloudUserDetailsServiceAdministratorRestTest extends AbstractOwncloudUserDetailsServiceRestTest {

  @Override
  public final String getBasicAuthorizationHeader() {
    return getDefaultBasicAuthorizationHeader();
  }

}
