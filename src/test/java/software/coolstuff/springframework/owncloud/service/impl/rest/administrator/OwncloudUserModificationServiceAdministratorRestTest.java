package software.coolstuff.springframework.owncloud.service.impl.rest.administrator;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserModificationServiceRestTest;

@ActiveProfiles("ADMINISTRATOR-URL")
public class OwncloudUserModificationServiceAdministratorRestTest extends AbstractOwncloudUserModificationServiceRestTest {

  @Override
  public String getBasicAuthorizationHeader() {
    return getDefaultBasicAuthorizationHeader();
  }

}
