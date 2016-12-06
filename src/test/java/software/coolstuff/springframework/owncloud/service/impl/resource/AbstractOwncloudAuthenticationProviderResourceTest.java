package software.coolstuff.springframework.owncloud.service.impl.resource;

import java.io.IOException;

import org.springframework.core.io.Resource;

import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudAuthenticationProviderTest;

public abstract class AbstractOwncloudAuthenticationProviderResourceTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected void prepareTestAuthenticate_OK(Credentials credentials) throws IOException {}

  @Override
  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws Exception {}

  @CompareResourceAfter("testOK")
  public void compareAfterTestOK(Resource target) throws Exception {
    compareResources(target);
  }

  @CompareResourceAfter("testNOK")
  public void compareAfterTestNOK(Resource target) throws Exception {
    compareResources(target);
  }
}
