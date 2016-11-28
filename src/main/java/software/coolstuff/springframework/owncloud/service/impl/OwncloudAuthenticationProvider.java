package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder) {
    super(builder, false);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    OwncloudUserDetails owncloudUserDetails = null;
    if (isRestAvailable()) {
      OcsUserInformation ocsUserInformation = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(username, password), OcsUserInformation.class, username);
      owncloudUserDetails = userDetailsService.loadPreloadedUserByUsername(username, ocsUserInformation);
    } else {
      if (resourceService == null) {
        throw new BadCredentialsException("resourceService is not available");
      }
      if (!resourceService.authenticate(username, password)) {
        throw new BadCredentialsException("wrong password or user not found");
      }
      owncloudUserDetails = userDetailsService.loadUserByUsernameFromResourceService(username);
    }
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

}
