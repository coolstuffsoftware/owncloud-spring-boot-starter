package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@Configuration
@ConditionalOnClass({ RestTemplateBuilder.class, MappingJackson2XmlHttpMessageConverter.class })
@ConditionalOnProperty(prefix = "owncloud", name = "location")
@EnableConfigurationProperties(OwncloudProperties.class)
public class OwncloudAutoConfiguration {

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
