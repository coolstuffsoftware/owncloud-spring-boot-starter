package software.coolstuff.springframework.owncloud.service.impl;

import org.junit.Test;

public class OwncloudResourceServiceTest {

  @Test(expected = IllegalStateException.class)
  public void testCheckGroupReferences_NOK() throws Exception {
    OwncloudResourceService resourceService = new OwncloudResourceService() {
      @Override
      public void afterPropertiesSet() throws Exception {
        checkGroupReferences(
            OwncloudResourceData.builder()
                .user(
                    OwncloudResourceData.User.builder()
                        .group(new OwncloudResourceData.Group("group1"))
                        .group(new OwncloudResourceData.Group("group2"))
                        .build())
                .group(new OwncloudResourceData.Group("group1"))
                .build());
      }
    };
    resourceService.afterPropertiesSet();
  }
}
