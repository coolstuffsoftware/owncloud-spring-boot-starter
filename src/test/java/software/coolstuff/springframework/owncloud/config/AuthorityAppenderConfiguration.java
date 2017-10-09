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
package software.coolstuff.springframework.owncloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Profile({ "LOCAL-AUTHORITY-APPENDER-USER-SERVICE", "REST-AUTHORITY-APPENDER-USER-SERVICE" })
public class AuthorityAppenderConfiguration {

  @Bean
  public OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper() {
    return (username, grantedAuthorities) -> {
      Set<GrantedAuthority> authorities = new HashSet<>(grantedAuthorities);
      authorities.add(new SimpleGrantedAuthority("group98"));
      authorities.add(new SimpleGrantedAuthority("group99"));
      return authorities;
    };
  }

  @Bean
  public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    return grantedAuthorities -> {
      throw new IllegalStateException("this Mapper should never been used");
    };
  }

}
