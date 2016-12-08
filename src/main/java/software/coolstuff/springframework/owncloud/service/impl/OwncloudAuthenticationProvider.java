package software.coolstuff.springframework.owncloud.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder) {
    super(builder, false, new OwncloudAuthenticationProviderResponseErrorHandler());
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    OwncloudUserDetails owncloudUserDetails = null;
    if (isRestAvailable()) {
      Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(username, password), Ocs.User.class, username);
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));
      owncloudUserDetails = userDetailsService.loadPreloadedUserByUsername(username, user);
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
  protected void checkFailure(String uri, Ocs.Meta metaInformation) throws OwncloudStatusException {
    if ("ok".equals(metaInformation.getStatus())) {
      return;
    }
    throw new BadCredentialsException(metaInformation.getMessage());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

  @NoArgsConstructor
  private static class OwncloudAuthenticationProviderResponseErrorHandler extends DefaultOwncloudResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      HttpStatus statusCode = response.getStatusCode();
      if (HttpStatus.UNAUTHORIZED.compareTo(statusCode) == 0) {
        throw new BadCredentialsException("User not found or wrong password");
      }
      super.handleError(response);
    }

  }
}
