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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudGrantedAuthoritiesMappingService;

import java.net.MalformedURLException;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass({
    RestTemplateBuilder.class,
    MappingJackson2XmlHttpMessageConverter.class
})
@ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
@EnableConfigurationProperties(OwncloudRestProperties.class)
public class OwncloudRestAutoConfiguration {

  private final RestTemplateBuilder restTemplateBuilder;
  private final OwncloudRestProperties owncloudRestProperties;
  private final OwncloudGrantedAuthoritiesMappingService owncloudGrantedAuthoritiesMappingService;

  @Bean
  public OwncloudRestUserServiceExtension owncloudUserRestService() {
    return new OwncloudRestUserServiceImpl(restTemplateBuilder, owncloudRestProperties);
  }

  @Bean
  public OwncloudGroupService owncloudGroupRestService() {
    return new OwncloudRestGroupServiceImpl(restTemplateBuilder, owncloudRestProperties);
  }

  @Bean
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudRestAuthenticationProviderImpl.class)
  public OwncloudRestAuthenticationProviderImpl owncloudRestAuthenticationProvider() {
    return new OwncloudRestAuthenticationProviderImpl(restTemplateBuilder, owncloudRestProperties, owncloudRestUserDetailsService(), owncloudGrantedAuthoritiesMappingService);
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudRestUserDetailsServiceImpl.class)
  public OwncloudRestUserDetailsService owncloudRestUserDetailsService() {
    return new OwncloudRestUserDetailsServiceImpl(restTemplateBuilder, owncloudRestProperties);
  }

  @Bean
  public OwncloudResourceService owncloudResourceService() throws MalformedURLException {
    return new OwncloudRestResourceServiceImpl(restTemplateBuilder, owncloudRestProperties, sardineCacheLoader(), owncloudUserRestService());
  }

  @Bean
  @ConditionalOnMissingBean(SardineCacheLoader.class)
  public SardineCacheLoader sardineCacheLoader() {
    return new SardineCacheLoader();
  }
}
