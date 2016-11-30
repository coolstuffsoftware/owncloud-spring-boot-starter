package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;

import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

@RestClientTest(OwncloudUserModificationService.class)
public abstract class AbstractOwncloudUserModificationServiceRestTest extends AbstractOwncloudRestTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Override
  protected AbstractOwncloudServiceImpl owncloudService() {
    return (OwncloudUserModificationServiceImpl) userModificationService;
  }

  @Override
  protected String getResourcePrefix() {
    return "/modificationService";
  }

}
