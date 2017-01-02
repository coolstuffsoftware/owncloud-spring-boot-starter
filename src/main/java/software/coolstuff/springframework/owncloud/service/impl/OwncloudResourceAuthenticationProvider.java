package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

@RequiredArgsConstructor
public class OwncloudResourceAuthenticationProvider implements AuthenticationProvider {

  private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private final OwncloudResourceService resourceService;

  @Autowired
  private OwncloudResourceUserDetailsService userDetailsService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (StringUtils.isBlank(authentication.getName())) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    if (authentication.getCredentials() == null) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    OwncloudResourceData.User user = resourceService.getUser(username);
    if (user == null) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }
    if (!user.isEnabled()) {
      throw new DisabledException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "Disabled"));
    }

    if (!StringUtils.equals(password, user.getPassword())) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));

    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetailsService.loadUserByUsername(username);
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OwncloudUtils.isAuthenticationClassSupported(authentication);
  }

}
