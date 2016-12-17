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
package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@Configuration
@ConditionalOnClass({
    RestTemplateBuilder.class,
    MappingJackson2XmlHttpMessageConverter.class,
    FormHttpMessageConverter.class
})
@ConditionalOnProperty(prefix = "owncloud", name = "location")
@EnableConfigurationProperties(OwncloudProperties.class)
public class OwncloudAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(FormHttpMessageConverter.class)
  public FormHttpMessageConverter formHttpMessageConverter() {
    return new FormHttpMessageConverter();
  }

  @Bean
  public OwncloudUserQueryService owncloudUserQueryService(RestTemplateBuilder builder) {
    return new OwncloudUserQueryServiceImpl(builder);
  }

  @Bean
  public OwncloudUserModificationService owncloudUserModificationService(RestTemplateBuilder builder) {
    return new OwncloudUserModificationServiceImpl(builder);
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudAuthenticationProvider.class)
  public OwncloudAuthenticationProvider owncloudAuthenticationProvider(RestTemplateBuilder builder) {
    return new OwncloudAuthenticationProvider(builder);
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudUserDetailsService.class)
  public OwncloudUserDetailsService owncloudUserDetailsService(RestTemplateBuilder builder) {
    return new OwncloudUserDetailsService(builder);
  }

  @Bean
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  @ConditionalOnMissingBean(OwncloudResourceService.class)
  public OwncloudResourceService owncloudResourceService() {
    return new OwncloudResourceService();
  }

}
