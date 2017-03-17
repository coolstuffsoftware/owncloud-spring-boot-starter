package software.coolstuff.springframework.owncloud.service.impl.local;

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
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@RequiredArgsConstructor
@Slf4j
class OwncloudLocalAuthenticationProvider implements AuthenticationProvider {

  private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private final OwncloudLocalUserDataService localDataService;

  @Autowired
  private OwncloudLocalUserDetailsService userDetailsService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    if (StringUtils.isBlank(username)) {
      log.error("Username is null or empty");
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : null;
    if (StringUtils.isBlank(password)) {
      log.error("Password is null or empty");
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    log.debug("Get Information about User {} from the Resource Service", username);
    OwncloudLocalUserData.User user = localDataService.getUser(username);
    if (user == null) {
      log.error("User {} has not been found", username);
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }
    if (!user.isEnabled()) {
      log.error("User {} is disabled", username);
      throw new DisabledException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "Disabled"));
    }

    if (!StringUtils.equals(password, user.getPassword())) {
      log.error("Wrong Password of User {}", username);
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    log.debug("Set a new UsernamePasswordAuthenticationToken with User {} to the SecurityContextHolder", username);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));

    log.info("User {} has been successfully authenticated. Get Information from UserDetailsService", username);
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetailsService.loadUserByUsername(username);
    log.trace("Set the Password of User {} to the Authentication Object", username);
    owncloudUserDetails.setPassword(password);

    return new UsernamePasswordAuthenticationToken(owncloudUserDetails, password, owncloudUserDetails.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OwncloudUtils.isAuthenticationClassSupported(authentication);
  }

}
