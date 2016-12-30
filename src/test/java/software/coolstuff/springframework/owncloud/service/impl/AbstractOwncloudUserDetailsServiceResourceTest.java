package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.core.userdetails.UserDetailsService;

public abstract class AbstractOwncloudUserDetailsServiceResourceTest extends AbstractOwncloudUserDetailsServiceTest {

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudResourceService.class;
  }

}
