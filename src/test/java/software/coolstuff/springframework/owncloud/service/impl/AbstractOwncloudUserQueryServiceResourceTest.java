package software.coolstuff.springframework.owncloud.service.impl;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

public abstract class AbstractOwncloudUserQueryServiceResourceTest extends AbstractOwncloudUserQueryServiceTest {

  @Override
  protected Class<? extends OwncloudUserQueryService> getUserQueryServiceClass() {
    return OwncloudUserQueryResourceServiceImpl.class;
  }

}
