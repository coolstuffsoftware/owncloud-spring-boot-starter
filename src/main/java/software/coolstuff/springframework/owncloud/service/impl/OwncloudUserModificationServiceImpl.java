package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;

import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

class OwncloudUserModificationServiceImpl extends AbstractOwncloudServiceImpl implements OwncloudUserModificationService {

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  OwncloudUserModificationServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

}
