package software.coolstuff.springframework.owncloud.service.impl;

import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ActiveProfiles;

@RestClientTest(OwncloudAuthenticationProvider.class)
@ActiveProfiles("RESOURCE-TEST")
public class OwncloudAuthenticationProviderResourceTest extends AbstractOwncloudRestTest {

  @Override
  protected String getResourcePrefix() {
    return "/authentication";
  }

  @Test
  public void testOK() {}

}
