package software.coolstuff.springframework.owncloud.service.impl.local;

import org.springframework.security.core.userdetails.UserDetailsService;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalUserDetailsService;

public abstract class AbstractLocalOwncloudUserDetailsServiceTest extends AbstractOwncloudUserDetailsServiceTest {

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudLocalUserDetailsService.class;
  }

}
