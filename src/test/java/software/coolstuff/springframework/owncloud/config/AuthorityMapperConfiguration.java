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
package software.coolstuff.springframework.owncloud.config;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile({ "LOCAL-AUTHORITY-MAPPER-USER-SERVICE", "REST-AUTHORITY-MAPPER-USER-SERVICE" })
public class AuthorityMapperConfiguration {

  @Bean
  public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    return (grantedAuthorities) -> {
      if (CollectionUtils.isEmpty(grantedAuthorities)) {
        return grantedAuthorities;
      }

      List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
      for (GrantedAuthority grantedAuthority : grantedAuthorities) {
        mappedAuthorities.add(new SimpleGrantedAuthority("MAPPED_" + grantedAuthority.getAuthority()));
      }
      return mappedAuthorities;
    };
  }

}
