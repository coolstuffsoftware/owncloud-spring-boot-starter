package software.coolstuff.springframework.owncloud.service.impl.local;

import org.springframework.security.authentication.AuthenticationProvider;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudAuthenticationProviderTest;

public abstract class AbstractLocalOwncloudAuthenticationProviderTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected Class<? extends AuthenticationProvider> getAuthenticationProviderClass() {
    return OwncloudLocalAuthenticationProviderImpl.class;
  }

}
