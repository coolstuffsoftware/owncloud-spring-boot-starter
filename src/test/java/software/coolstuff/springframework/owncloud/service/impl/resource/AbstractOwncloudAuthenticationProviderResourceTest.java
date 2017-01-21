package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.springframework.security.authentication.AuthenticationProvider;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudAuthenticationProviderTest;
import software.coolstuff.springframework.owncloud.service.impl.resource.OwncloudResourceAuthenticationProvider;

public abstract class AbstractOwncloudAuthenticationProviderResourceTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected Class<? extends AuthenticationProvider> getAuthenticationProviderClass() {
    return OwncloudResourceAuthenticationProvider.class;
  }

}
