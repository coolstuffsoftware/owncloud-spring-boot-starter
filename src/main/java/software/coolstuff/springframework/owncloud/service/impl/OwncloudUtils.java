/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl;

import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudNoFileResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

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
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authenticationClass);
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
   * Checks, if the given Authentication Object is authenticated
   * by the Owncloud{Rest|Local}AuthenticationProvider
   * @param authentication Authentication Object
   * @return is authenticated by the Owncloud{Rest|Local}AuthenticationProvider
   */
  public static boolean isValidAuthentication(Authentication authentication) {
    if (authentication == null) {
      return false;
    }

    // if UserDetails are set then it must be of Class OwncloudUserDetails
    if (authentication.getDetails() != null && !ClassUtils.isAssignable(authentication.getPrincipal().getClass(), OwncloudUserDetails.class)) {
      return false;
    }

    if (authentication.getCredentials() != null) {
      // if Credentials are available then these must be of Class CharSequence and not empty
      return CharSequence.class.isAssignableFrom(authentication.getCredentials().getClass()) &&
          StringUtils.isNotBlank((CharSequence) authentication.getCredentials());
    }

    if (authentication.getPrincipal() != null) {
      // Password of the UserDetails must not be empty
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      return StringUtils.isNotBlank(userDetails.getPassword());
    }

    return false;
  }

  /**
   * Checks, if the given Authentication Object is NOT authenticated
   * by the Owncloud{Rest|Local}AuthenticationProvider
   * @param authentication Authentication Object
   * @return is not authenticated by the Owncloud{Rest|Local}AuthenticationProvider
   */
  public static boolean isInvalidAuthentication(Authentication authentication) {
    return !isValidAuthentication(authentication);
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
      throw new OwncloudNoFileResourceException(owncloudResource.getHref());
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
        Optional.ofNullable(owncloudResource)
            .map(resource -> resource.getMediaType().toString())
            .orElse(null));
  }

  /**
   * Checks, if the OwncloudResource is not a Directory
   * @param owncloudResource OwncloudResource
   * @return is it not a Directory
   */
  public static boolean isNotDirectory(OwncloudResource owncloudResource) {
    return !isDirectory(owncloudResource);
  }

  /**
   * Get the MediaType of a Directory (<code>httpd/unix-directory</code>)
   * @return MediaType of a Directory
   */
  public static MediaType getDirectoryMediaType() {
    return MediaType.valueOf(UNIX_DIRECTORY);
  }
}
