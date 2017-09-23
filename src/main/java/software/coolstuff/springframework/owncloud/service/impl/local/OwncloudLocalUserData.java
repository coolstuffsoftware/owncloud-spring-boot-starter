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
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.Collection;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Singular;

@lombok.Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder
@XmlRootElement(name = "owncloud")
@XmlAccessorType(XmlAccessType.FIELD)
class OwncloudLocalUserData {

  @lombok.Data
  @lombok.EqualsAndHashCode(of = "username")
  @lombok.ToString(exclude = "password")
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @lombok.Builder
  @XmlRootElement(name = "user")
  @XmlAccessorType(XmlAccessType.FIELD)
  static class User {

    @NotNull
    private String username;
    private String password;
    private boolean enabled = true;
    private String displayname;
    private String email;
    @Min(0)
    private Long quota;

    @lombok.Singular
    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "group")
    private Set<String> groups;
  }

  @lombok.Singular
  @XmlElementWrapper(name = "users")
  @XmlElement(name = "user")
  private Collection<User> users;

  @Singular
  @XmlElementWrapper(name = "groups")
  @XmlElement(name = "group")
  private Set<String> groups;
}
