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
package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

/**
 * Thrown when trying to authenticate neither with {@link OwncloudAuthentication} nor with {@link UsernamePasswordAuthenticationToken}
 *
 * @author mufasa1976
 */
@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Invalid Authentication Object")
public class OwncloudInvalidAuthenticationObjectException extends AuthenticationException {

  private static final long serialVersionUID = -3421422373807935754L;

  public OwncloudInvalidAuthenticationObjectException(Authentication authentication) {
    super(
        "Authentication is not of type " +
            OwncloudAuthentication.class.getSimpleName() +
            ". Instead it is of Type " +
            (authentication == null ? "null" : authentication.getClass().getSimpleName()));
  }

}
