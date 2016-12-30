package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.authentication.AuthenticationProvider;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudAuthenticationProviderTest;

public abstract class AbstractOwncloudAuthenticationProviderResourceTest extends AbstractOwncloudAuthenticationProviderTest {

  @Override
  protected Class<? extends AuthenticationProvider> getAuthenticationProviderClass() {
    return OwncloudResourceService.class;
  }

}
