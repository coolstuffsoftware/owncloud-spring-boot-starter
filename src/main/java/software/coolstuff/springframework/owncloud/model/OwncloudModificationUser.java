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

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OwncloudModificationUser {

  private String username;
  private String password;

  private boolean enabled = true;

  private String displayName;
  private String email;

  @Singular("group")
  private List<String> groups = new ArrayList<>();

  public static class OwncloudModificationUserBuilder {

    private boolean enabled = true;
  }

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
