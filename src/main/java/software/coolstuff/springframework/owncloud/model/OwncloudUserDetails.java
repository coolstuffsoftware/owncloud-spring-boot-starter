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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

/**
 * Implementation of the {@link UserDetails} Specification of Spring Security.
 * <p/>
 * An Instance of this Class will be returned by <code>OwncloudUserDetailsService#loadUserByUsername(String)</code>.
 *
 * @author mufasa1976
 * @see UserDetails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "username")
@ToString(exclude = "password")
@Builder
public class OwncloudUserDetails implements UserDetails {

  private static final long serialVersionUID = 7384295040126418671L;

  /**
   * Username of the authenticated User.
   * @param username Username of the authenticated User
   * @return Username of the authenticated User
   */
  @NotNull
  private String username;

  /**
   * Password of the authenticated User.
   * <p/>
   * <u>Note:</u> The Password will be hidden when calling {@link #toString()}
   * @param password Password of the authenticated User
   * @return Password of the authenticated User
   */
  private String password;

  /**
   * State of the authenticated User (enabled/disabled)
   * @param enabled enable or disable the authenticated User
   * @return is the authenticated User enabled (<code>true</code>) or disabled (<code>false</code>)
   */
  private boolean enabled = true;

  /**
   * Granted Authorities of the authenticated User.
   * @param authorities granted Authorities of the authenticated User
   * @return granted Authorities of the authenticated User
   */
  private Collection<? extends GrantedAuthority> authorities;

  /**
   * Owncloud Groups of the authenticated User.
   * <p/>
   * If neither {@link OwncloudGrantedAuthoritiesMapper} nor {@link GrantedAuthoritiesMapper} will be used then
   * this is the same as {@link #getAuthorities()}
   * @param groups Owncloud Groups of the authenticated User
   * @return Owncloud Groups of the authenticated User
   */
  private List<String> groups = new ArrayList<>();

  /**
   * Display Name of the authenticated User.
   * @param displayName Display Name of the authenticated User
   * @return Display Name of the authenticated User
   */
  private String displayName;

  /**
   * Email of the authenticated User.
   * @param email Email of the authenticated User
   * @return Email of the authenticated User
   */
  private String email;

  public static class OwncloudUserDetailsBuilder {
    private boolean enabled = true;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
