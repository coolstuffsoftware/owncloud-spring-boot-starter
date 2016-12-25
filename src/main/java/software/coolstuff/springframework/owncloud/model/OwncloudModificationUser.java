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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.GrantedAuthority;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

/**
 * This Class wil be used for any User Modifications by {@link OwncloudUserModificationService#saveUser(OwncloudModificationUser)}
 *
 * @author mufasa1976
 * @see OwncloudUserDetailsService#loadUserByUsername(String)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OwncloudModificationUser {

  /**
   * Username of the User to be modified.
   * @param username Username of the User to be modified
   * @return Username of the User to be modified
   */
  private String username;

  /**
   * Password of the new User.
   * <p/>
   * This Paramter will only be used, when the User will be created
   * @param password Password of the new User
   * @return Password of the new User
   */
  private String password;

  /**
   * State of the User to be modified.
   * @param enabled set the User to be enabled (<code>true</code>) or disabled (<code>false</code>)
   * @return is the User enabled (<code>true</code>) or disabled (<code>false</code>)
   */
  private boolean enabled = true;

  /**
   * Display Name of the User to be modified.
   * @param displayName Display Name of the User to be modified
   * @return modified Display Name
   */
  private String displayName;

  /**
   * Email of the User to be modified.
   * @param email Email of the User to be modified
   * @return modified Email
   */
  private String email;

  /**
   * Group Memberships of the User to be modified.
   * @param groups Group Memberships of the User to be modified
   * @return modified Group Memberships
   */
  @Singular("group")
  private List<String> groups = new ArrayList<>();

  public static class OwncloudModificationUserBuilder {
    private boolean enabled = true;
  }

  /**
   * Constructor by any existing {@link OwncloudUserDetails} Object.
   * <p/>
   * A {@link OwncloudUserDetails} Object will be returned by the {@link OwncloudUserDetailsService#loadUserByUsername(String)}
   * during the Authentication Process of the {@link OwncloudAuthenticationProvider#authenticate(org.springframework.security.core.Authentication)}
   * and resist as a {@link Principal} within the {@link OwncloudAuthentication} Object (returned by {@link OwncloudAuthentication#getPrincipal()}
   * @param userDetails existing {@link OwncloudUserDetails} Object
   */
  public OwncloudModificationUser(OwncloudUserDetails userDetails) {
    Validate.notNull(userDetails);

    setUsername(userDetails.getUsername());
    setPassword(userDetails.getPassword());

    setEnabled(userDetails.isEnabled());

    setDisplayName(userDetails.getDisplayName());
    setEmail(userDetails.getEmail());

    if (CollectionUtils.isNotEmpty(userDetails.getAuthorities())) {
      for (GrantedAuthority authority : userDetails.getAuthorities()) {
        groups.add(authority.getAuthority());
      }
    }
  }
}
