package software.coolstuff.springframework.owncloud.service.impl.local.file;

import org.springframework.test.context.ActiveProfiles;
import software.coolstuff.springframework.owncloud.service.impl.local.AbstractLocalOwncloudUserServiceQueryTest;

@ActiveProfiles("LOCAL-FILE-USER-SERVICE")
public class OwncloudLocalUserServiceQueryFileTest extends AbstractLocalOwncloudUserServiceQueryTest implements OwncloudLocalModifyingFileTest {

  @Override
  public String getResourcePrefix() {
    return "/modificationService";
  }

}
