package software.coolstuff.springframework.owncloud.service.impl.resource;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.resource.OwncloudUserQueryResourceServiceImpl;

public abstract class AbstractOwncloudUserQueryServiceResourceTest extends AbstractOwncloudUserQueryServiceTest {

  @Override
  protected Class<? extends OwncloudUserQueryService> getUserQueryServiceClass() {
    return OwncloudUserQueryResourceServiceImpl.class;
  }

}
