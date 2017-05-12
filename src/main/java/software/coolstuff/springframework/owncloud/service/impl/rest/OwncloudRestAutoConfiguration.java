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

import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupQueryService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;

@Configuration
@ConditionalOnClass({
    RestTemplateBuilder.class,
    MappingJackson2XmlHttpMessageConverter.class
})
@ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
@EnableConfigurationProperties(OwncloudRestProperties.class)
public class OwncloudRestAutoConfiguration {

  @Bean
  public OwncloudRestUserQueryService owncloudUserQueryRestService(RestTemplateBuilder builder) {
    return new OwncloudRestUserQueryServiceImpl(builder);
  }

  @Bean
  public OwncloudGroupQueryService owncloudGroupQueryService(RestTemplateBuilder builder) {
    return new OwncloudGroupQueryRestServiceImpl(builder);
  }

  @Bean
  public OwncloudUserService owncloudUserRestService(RestTemplateBuilder builder) {
    return new OwncloudUserRestServiceImpl(builder);
  }

  @Bean
  public OwncloudGroupService owncloudGroupRestService(RestTemplateBuilder builder) {
    return new OwncloudGroupRestServiceImpl(builder);
  }

  @Bean
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudRestAuthenticationProviderImpl.class)
  public OwncloudRestAuthenticationProviderImpl owncloudRestAuthenticationProvider(RestTemplateBuilder builder) {
    return new OwncloudRestAuthenticationProviderImpl(builder);
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudRestUserDetailsServiceImpl.class)
  public OwncloudRestUserDetailsServiceImpl owncloudRestUserDetailsService(RestTemplateBuilder builder) {
    return new OwncloudRestUserDetailsServiceImpl(builder);
  }

  @Bean
  public OwncloudResourceService owncloudResourceService(RestTemplateBuilder builder, OwncloudRestProperties properties) throws MalformedURLException {
    return new OwncloudRestResourceServiceImpl(builder, properties);
  }

  @Bean
  @ConditionalOnMissingBean(SardineCacheLoader.class)
  public SardineCacheLoader sardineCacheLoader() {
    return new SardineCacheLoader();
  }
}
