package software.coolstuff.springframework.owncloud.service.impl.local;

import org.springframework.security.authentication.AuthenticationProvider;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudAuthenticationProviderTest;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalAuthenticationProvider;

public abstract class AbstractOwncloudAuthenticationProviderLocalTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected Class<? extends AuthenticationProvider> getAuthenticationProviderClass() {
    return OwncloudLocalAuthenticationProvider.class;
  }

}
