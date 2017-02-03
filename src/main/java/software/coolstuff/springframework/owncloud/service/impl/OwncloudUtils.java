package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.google.common.base.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudNoFileResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;

/**
 * General Utilities for the Owncloud Services.
 *
 * @author mufasa1976
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OwncloudUtils {
  private static final String UNIX_DIRECTORY = "httpd/unix-directory";

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

  /**
   * Convert a OwncloudResource to a OwncloudFileResource.
   * @param owncloudResource OwncloudResource
   * @return converted to OwncloudFileResource
   * @throws OwncloudNoFileResourceException if the OwncloudResource is not convertable to a OwncloudFileResource
   */
  public static OwncloudFileResource toOwncloudFileResource(OwncloudResource owncloudResource) throws OwncloudNoFileResourceException {
    if (owncloudResource == null) {
      return null;
    }
    if (isDirectory(owncloudResource) || !ClassUtils.isAssignable(owncloudResource.getClass(), OwncloudFileResource.class)) {
      throw new OwncloudNoFileResourceException();
    }
    return (OwncloudFileResource) owncloudResource;
  }

  /**
   * Checks, if the OwncloudResource is a Directory
   * @param owncloudResource OwncloudResource
   * @return is it a Directory
   */
  public static boolean isDirectory(OwncloudResource owncloudResource) {
    return UNIX_DIRECTORY.equals(
        Optional.fromNullable(owncloudResource)
            .transform(resource -> resource.getMediaType().toString())
            .orNull());
  }

  public static MediaType getDirectoryMediaType() {
    return MediaType.valueOf(UNIX_DIRECTORY);
  }
}
