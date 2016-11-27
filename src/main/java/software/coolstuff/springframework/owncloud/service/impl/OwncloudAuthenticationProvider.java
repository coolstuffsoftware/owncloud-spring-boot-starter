package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder) {
    super(builder, false);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    final HttpEntity<String> request = new HttpEntity<>(prepareHeaderWithBasicAuthorization(username, password));

    OcsUserInformation ocsUserInformation = exchange("/cloud/users/{user}", HttpMethod.GET, request, OcsUserInformation.class, username);

    OwncloudUserDetails owncloudUserDetails = userDetailsService.loadPreloadedUserByUsername(username, ocsUserInformation);
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

}
