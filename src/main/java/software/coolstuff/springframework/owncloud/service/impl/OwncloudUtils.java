package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OwncloudUtils {

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
