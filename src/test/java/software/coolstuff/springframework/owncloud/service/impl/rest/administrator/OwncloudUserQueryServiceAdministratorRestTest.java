package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserQueryServiceRestTest;

@ActiveProfiles("ADMINISTRATOR-URL")
public class OwncloudUserQueryServiceAdministratorRestTest extends AbstractOwncloudUserQueryServiceRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getDefaultBasicAuthorizationHeader();
  }

}
