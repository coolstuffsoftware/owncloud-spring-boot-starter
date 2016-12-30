package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OwncloudUtils {

  static boolean isNoResourceLocation(String location) {
    return !isResourceLocation(location);
  }

  static boolean isResourceLocation(String location) {
    return StringUtils.startsWith(location, "file:") || StringUtils.startsWith(location, "classpath:");
  }

  static boolean isFileResourceLocation(String location) {
    return StringUtils.startsWith(location, "file:");
  }

  static boolean isAuthenticationClassSupported(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

  static void validateUserNotNull(OwncloudResourceData.User user, String username) {
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
  }

  static void validateGroupNotNull(OwncloudResourceData.Group group, String groupname) {
    if (group == null) {
      throw new OwncloudGroupNotFoundException(groupname);
    }
  }

}
