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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

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
@EnableAspectJAutoProxy
class OwncloudAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(FormHttpMessageConverter.class)
  public FormHttpMessageConverter formHttpMessageConverter() {
    return new FormHttpMessageConverter();
  }

  @Bean
  public OwncloudUserModificationChecker owncloudUserModificationChecker(OwncloudProperties owncloudProperties) {
    return new OwncloudUserModificationChecker(owncloudProperties);
  }

  @Bean
  public OwncloudUserDetailsConversionService owncloudUserDetailsConversionService() {
    return new OwncloudUserDetailsConversionService();
  }

  @Bean("owncloudUserQueryService")
  @ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
  public OwncloudUserQueryService owncloudUserQueryRestService(RestTemplateBuilder builder) {
    return new OwncloudUserQueryRestServiceImpl(builder);
  }

  @Bean("owncloudUserQueryService")
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  public OwncloudUserQueryService owncloudUserQueryResourceService() {
    return new OwncloudUserQueryResourceServiceImpl(owncloudResourceService());
  }

  @Bean("owncloudUserModificationService")
  @ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
  public OwncloudUserModificationService owncloudUserModificationRestService(RestTemplateBuilder builder) {
    return new OwncloudUserModificationRestServiceImpl(builder);
  }

  @Bean("owncloudUserModificationService")
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  public OwncloudUserModificationService owncloudUserModificationResourceService() {
    return new OwncloudUserModificationResourceService(owncloudResourceService());
  }

  @Bean("owncloudAuthenticationProvider")
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudRestAuthenticationProvider.class)
  @ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
  public OwncloudRestAuthenticationProvider owncloudRestAuthenticationProvider(RestTemplateBuilder builder) {
    return new OwncloudRestAuthenticationProvider(builder);
  }

  @Bean("owncloudAuthenticationProvider")
  @Qualifier("owncloudAuthenticationProvider")
  @ConditionalOnMissingBean(OwncloudResourceAuthenticationProvider.class)
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  public OwncloudResourceAuthenticationProvider owncloudResourceAuthenticationProvider() {
    return new OwncloudResourceAuthenticationProvider(owncloudResourceService());
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudRestUserDetailsService.class)
  @ConditionalOnExpression("#{!('${owncloud.location}' matches 'file:.*') and !('${owncloud.location}' matches 'classpath:.*')}")
  public OwncloudRestUserDetailsService owncloudRestUserDetailsService(RestTemplateBuilder builder) {
    return new OwncloudRestUserDetailsService(builder);
  }

  @Bean
  @Qualifier("owncloudUserDetailsService")
  @ConditionalOnMissingBean(OwncloudResourceUserDetailsService.class)
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  public OwncloudResourceUserDetailsService owncloudResourceUserDetailsService() {
    return new OwncloudResourceUserDetailsService(owncloudResourceService());
  }

  @Bean
  @ConditionalOnMissingBean(OwncloudResourceService.class)
  @ConditionalOnExpression("#{'${owncloud.location}' matches 'file:.*' or '${owncloud.location}' matches 'classpath:.*'}")
  public OwncloudResourceService owncloudResourceService() {
    return new OwncloudResourceService();
  }

}
