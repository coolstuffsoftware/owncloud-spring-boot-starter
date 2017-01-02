package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.core.userdetails.UserDetailsService;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;

public abstract class AbstractOwncloudUserDetailsServiceResourceTest extends AbstractOwncloudUserDetailsServiceTest {

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudResourceUserDetailsService.class;
  }

}
