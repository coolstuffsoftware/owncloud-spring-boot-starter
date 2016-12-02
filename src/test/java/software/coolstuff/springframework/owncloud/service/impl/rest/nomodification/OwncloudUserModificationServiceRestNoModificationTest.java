package software.coolstuff.springframework.owncloud.service.impl.rest.nomodification;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserModificationServiceRestTest;

@ActiveProfiles("NO-MODIFICATION-TEST")
public class OwncloudUserModificationServiceRestNoModificationTest
    extends AbstractOwncloudUserModificationServiceRestTest {

  @Override
  protected String getBasicAuthorizationHeader() {
    return "never used";
  }

}
