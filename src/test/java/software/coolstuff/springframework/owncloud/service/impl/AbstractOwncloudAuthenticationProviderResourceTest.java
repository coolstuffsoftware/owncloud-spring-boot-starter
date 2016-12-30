package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.authentication.AuthenticationProvider;

public abstract class AbstractOwncloudAuthenticationProviderResourceTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected Class<? extends AuthenticationProvider> getAuthenticationProviderClass() {
    return OwncloudResourceService.class;
  }

}
