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
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.Set;

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
    @Builder.Default
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
