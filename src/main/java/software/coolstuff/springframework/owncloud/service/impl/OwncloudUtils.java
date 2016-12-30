package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OwncloudUtils {

  /**
   * Checks, if the defined Location is a Resource.
   * <p/>
   * This will be done by checking, if the Location starts either with <code>file:</code> or <code>classpath</code>
   *
   * @param location
   *          Location to be checked
   * @return
   *         <ul>
   *         <li>true ... Location is a Resource</li>
   *         <li>false ... Location is possible a URL</li>
   *         </ul>
   */
  public static boolean isResourceInsteadOfUrl(String location) {
    return StringUtils.startsWith(location, "file:") || StringUtils.startsWith(location, "classpath:");
  }

  /**
   * Checks, if the defined Location is not a Resource.
   * <p/>
   * This will be done by checking, if the Location starts either with <code>file:</code> or <code>classpath</code>
   *
   * @param location
   *          Location to be checked
   * @return
   *         <ul>
   *         <li>true ... Location is possible a URL</li>
   *         <li>false ... Location is a Resource</li>
   *         </ul>
   */
  public static boolean isNoResource(String location) {
    return !isResourceInsteadOfUrl(location);
  }

  public static boolean isAuthenticationClassSupported(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

}
