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
package software.coolstuff.springframework.owncloud.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "owncloud", name = "location")
@EnableAspectJAutoProxy
public class OwncloudAutoConfiguration {

  private final OwncloudProperties owncloudProperties;

  @Bean
  public OwncloudAuthenticationTypeChecker owncloudUserModificationChecker() {
    return new OwncloudAuthenticationTypeChecker(owncloudProperties);
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudGrantedAuthoritiesMappingService.class)
  public OwncloudGrantedAuthoritiesMappingService owncloudUserDetailsMappingService(
      @Autowired(required = false) OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper,
      @Autowired(required = false) GrantedAuthoritiesMapper grantedAuthoritiesMapper) {
    return new OwncloudGrantedAuthoritiesMappingServiceImpl(owncloudGrantedAuthoritiesMapper, grantedAuthoritiesMapper);
  }
}
