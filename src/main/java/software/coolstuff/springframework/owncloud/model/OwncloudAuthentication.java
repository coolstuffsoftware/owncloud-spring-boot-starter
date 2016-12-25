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
package software.coolstuff.springframework.owncloud.model;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;

/**
 * Authentication Object which will be returned by {@link OwncloudAuthenticationProvider#authenticate(Authentication)}.
 *
 * @author mufasa1976
 */
@RequiredArgsConstructor
public class OwncloudAuthentication implements Authentication {

  private static final long serialVersionUID = -7867915541247069469L;

  @NotNull
  private final OwncloudUserDetails owncloudUserDetails;

  @Override
  public String getName() {
    return owncloudUserDetails.getUsername();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return owncloudUserDetails.getAuthorities();
  }

  @Override
  public Object getCredentials() {
    return owncloudUserDetails.getPassword();
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return owncloudUserDetails;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

}
