/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudGrantedAuthoritiesMappingService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@RequiredArgsConstructor
@Slf4j
public class OwncloudLocalAuthenticationProviderImpl implements AuthenticationProvider {

  private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private final OwncloudLocalUserDataService localDataService;
  private final OwncloudUserDetailsService userDetailsService;
  private final OwncloudGrantedAuthoritiesMappingService grantedAuthoritiesMappingService;

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

    return new UsernamePasswordAuthenticationToken(owncloudUserDetails, password, grantedAuthoritiesMappingService.mapGrantedAuthorities(owncloudUserDetails));
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OwncloudUtils.isAuthenticationClassSupported(authentication);
  }

}
