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
package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@Configuration
@ConditionalOnClass({
    MappingJackson2XmlHttpMessageConverter.class
})
@ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
@EnableConfigurationProperties(OwncloudResourceProperties.class)
class OwncloudResourceAutoConfiguration {

  @Bean
  public OwncloudUserQueryService owncloudUserQueryService() {
    return new OwncloudUserQueryResourceServiceImpl(owncloudResourceService());
  }

  @Bean
  public OwncloudUserModificationService owncloudUserModificationService() {
    return new OwncloudUserModificationResourceService(owncloudResourceService());
  }

  @Bean
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudResourceAuthenticationProvider.class)
  public OwncloudResourceAuthenticationProvider owncloudAuthenticationProvider() {
    return new OwncloudResourceAuthenticationProvider(owncloudResourceService());
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudResourceUserDetailsService.class)
  public OwncloudResourceUserDetailsService owncloudResourceUserDetailsService() {
    return new OwncloudResourceUserDetailsService(owncloudResourceService());
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudResourceService.class)
  public OwncloudResourceService owncloudResourceService() {
    return new OwncloudResourceService();
  }

}
