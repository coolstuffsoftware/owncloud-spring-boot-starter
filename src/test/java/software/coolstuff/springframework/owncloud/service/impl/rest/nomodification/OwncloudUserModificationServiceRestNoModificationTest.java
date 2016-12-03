package software.coolstuff.springframework.owncloud.service.impl.rest.nomodification;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudUserModificationServiceRestTest;

@ActiveProfiles("NO-MODIFICATION-URL-TEST")
public class OwncloudUserModificationServiceRestNoModificationTest
    extends AbstractOwncloudUserModificationServiceRestTest {

  @Override
  protected String getBasicAuthorizationHeader() {
    return "never used";
  }

  @Override
  @Test(expected = AccessDeniedException.class)
  public void testSaveUser_CreateUser_OK() throws MalformedURLException, IOException {
    super.testSaveUser_CreateUser_OK();
  }
}
