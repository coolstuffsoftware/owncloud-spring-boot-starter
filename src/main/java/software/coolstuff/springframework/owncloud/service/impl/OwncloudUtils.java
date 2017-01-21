package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

/**
 * General Utilities for the Owncloud Services.
 *
 * @author mufasa1976
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OwncloudUtils {

  /**
   * Checks, if the given Authentication Class is expected to be valid
   * to be used by the Owncloud AuthenticationProvider Implementations.
   * @param authentication Class of the Authentication Object
   * @return is the Authentication Object be servicable by the Owncloud AuthenticationProvider Implementation
   */
  public static boolean isAuthenticationClassSupported(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

}
