package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.springframework.security.core.userdetails.UserDetailsService;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.resource.OwncloudResourceUserDetailsService;

public abstract class AbstractOwncloudUserDetailsServiceResourceTest extends AbstractOwncloudUserDetailsServiceTest {

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudResourceUserDetailsService.class;
  }

}
