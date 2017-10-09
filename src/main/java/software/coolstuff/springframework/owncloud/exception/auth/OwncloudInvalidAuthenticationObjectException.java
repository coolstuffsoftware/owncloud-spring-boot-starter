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
package software.coolstuff.springframework.owncloud.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when not trying to authenticate with {@link UsernamePasswordAuthenticationToken}
 *
 * @author mufasa1976
 * @since 1.0.0
 */
@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Invalid Authentication Object")
public class OwncloudInvalidAuthenticationObjectException extends AuthenticationException {

  private static final long serialVersionUID = -3421422373807935754L;

  public OwncloudInvalidAuthenticationObjectException(Authentication authentication, Class<? extends Authentication> expectedAuthenticationClass) {
    super(
        "Authentication is not of type " +
            expectedAuthenticationClass.getSimpleName() +
            ". Instead it is of Type " +
            (authentication == null ? "null" : authentication.getClass().getSimpleName()));
  }

}
