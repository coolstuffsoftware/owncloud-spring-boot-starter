package software.coolstuff.springframework.owncloud.service.impl.local;

import org.springframework.security.core.userdetails.UserDetailsService;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalUserDetailsServiceImpl;

public abstract class AbstractLocalOwncloudUserDetailsImplServiceTest extends AbstractOwncloudUserDetailsServiceTest {

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudLocalUserDetailsServiceImpl.class;
  }

}
