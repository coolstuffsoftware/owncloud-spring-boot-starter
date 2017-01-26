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
   * Checks, if the given Authentication Class is expected valid
   * to be used by the Owncloud AuthenticationProvider Implementations.
   * @param authenticationClass Class of the Authentication Object
   * @return is the Authentication Object be servicable by the Owncloud AuthenticationProvider Implementation
   */
  public static boolean isAuthenticationClassSupported(Class<?> authenticationClass) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authenticationClass) ||
        OwncloudAuthentication.class.isAssignableFrom(authenticationClass);
  }

  /**
   * Checks, if the given Authentication Class is expected invalid
   * to be used by the Owncloud AuthenticationProvider Implementations.
   * @param authenticationClass Class of the Authentication Object
   * @return is the Authentication Object be not servicable by the Owncloud AuthenticationProvider Implementation
   */
  public static boolean isAuthenticationClassNotSupported(Class<?> authenticationClass) {
    return !isAuthenticationClassSupported(authenticationClass);
  }

}
