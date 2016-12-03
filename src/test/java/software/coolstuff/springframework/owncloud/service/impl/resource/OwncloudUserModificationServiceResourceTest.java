package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.AbstractOwncloudTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

@RestClientTest(OwncloudUserModificationService.class)
@ActiveProfiles("RESOURCE-TEST")
public class OwncloudUserModificationServiceResourceTest extends AbstractOwncloudTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Override
  protected final String getResourcePrefix() {
    return "/modificationService";
  }

  @Test
  public void testSaveUser() {
    // TODO: implement me !!!
  }
}
