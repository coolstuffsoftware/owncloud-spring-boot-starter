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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudGrantedAuthoritiesMappingService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
@ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
@EnableConfigurationProperties(OwncloudLocalProperties.class)
public class OwncloudLocalAutoConfiguration {

  private final Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;
  private final ResourceLoader resourceLoader;
  private final OwncloudProperties owncloudProperties;
  private final OwncloudGrantedAuthoritiesMappingService owncloudGrantedAuthoritiesMappingService;

  @Bean
  public OwncloudLocalUserServiceExtension owncloudLocalUserService() {
    return new OwncloudLocalUserServiceImpl(owncloudLocalUserDataService());
  }

  @Bean
  public OwncloudGroupService owncloudGroupService() {
    return new OwncloudLocalGroupServiceImpl(owncloudLocalUserDataService());
  }

  @Bean
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudLocalAuthenticationProviderImpl.class)
  public OwncloudLocalAuthenticationProviderImpl owncloudAuthenticationProvider() {
    return new OwncloudLocalAuthenticationProviderImpl(owncloudLocalUserDataService(), owncloudUserDetailsService(), owncloudGrantedAuthoritiesMappingService);
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudLocalUserDetailsServiceImpl.class)
  public OwncloudUserDetailsService owncloudUserDetailsService() {
    return new OwncloudLocalUserDetailsServiceImpl(owncloudLocalUserDataService());
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudLocalUserDataService.class)
  public OwncloudLocalUserDataService owncloudLocalUserDataService() {
    return new OwncloudLocalUserDataServiceImpl(jackson2ObjectMapperBuilder, resourceLoader, owncloudProperties);
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudLocalResourceChecksumService.class)
  @ConditionalOnProperty("owncloud.resource-service.location")
  public OwncloudLocalResourceChecksumService owncloudLocalResourceChecksumService() {
    return new OwncloudLocalResourceChecksumServiceImpl();
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudResourceService.class)
  @ConditionalOnProperty("owncloud.resource-service.location")
  public OwncloudLocalResourceService owncloudResourceService() {
    return new OwncloudLocalResourceServiceImpl();
  }

}
