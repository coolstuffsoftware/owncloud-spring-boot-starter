package software.coolstuff.springframework.owncloud.service.impl.local;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalUserQueryResourceImpl;

public abstract class AbstractLocalOwncloudUserQueryServiceTest extends AbstractOwncloudUserQueryServiceTest {

  @Override
  protected Class<? extends OwncloudUserQueryService> getUserQueryServiceClass() {
    return OwncloudLocalUserQueryResourceImpl.class;
  }

}
