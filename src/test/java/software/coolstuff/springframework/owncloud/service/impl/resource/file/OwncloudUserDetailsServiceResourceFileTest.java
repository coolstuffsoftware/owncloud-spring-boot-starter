package software.coolstuff.springframework.owncloud.service.impl.resource.file;

import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.config.OwncloudResourceFileTest;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;

@ActiveProfiles("RESOURCE-FILE-TEST")
public class OwncloudUserDetailsServiceResourceFileTest extends AbstractOwncloudUserDetailsServiceTest implements OwncloudResourceFileTest {

  @CompareResourceAfter("testOK")
  public void compareAfterTestOK(Resource target) throws Exception {
    compareResources(target);
  }

  @CompareResourceAfter("testNOK")
  public void compareAfterTestNOK(Resource target) throws Exception {
    compareResources(target);
  }

}
