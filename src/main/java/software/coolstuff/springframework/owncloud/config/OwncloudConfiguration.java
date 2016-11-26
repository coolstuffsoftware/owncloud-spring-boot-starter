package software.coolstuff.springframework.owncloud.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.service.api.OwncloudService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceImpl;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudServiceImpl;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@Configuration
@ConditionalOnClass({ RestTemplateBuilder.class, MappingJackson2XmlHttpMessageConverter.class })
@ConditionalOnProperty(prefix = "owncloud", name = "url")
@EnableConfigurationProperties(AbstractOwncloudServiceImpl.Properties.class)
public class OwncloudConfiguration {

  @Bean
  public OwncloudService owncloudService(
      RestTemplateBuilder builder,
      AbstractOwncloudServiceImpl.Properties properties,
      MappingJackson2XmlHttpMessageConverter messageConverter) {
    return new OwncloudServiceImpl(builder, properties, messageConverter);
  }

  @Bean
  @ConditionalOnProperty(prefix = "owncloud", name = "enable-authentication", havingValue = "true")
  public OwncloudAuthenticationProvider owncloudAuthenticationProvider(
      RestTemplateBuilder builder,
      AbstractOwncloudServiceImpl.Properties properties,
      MappingJackson2XmlHttpMessageConverter messageConverter) {
    return new OwncloudAuthenticationProvider(builder, properties, messageConverter);
  }

  @Bean
  @ConditionalOnProperty(prefix = "owncloud", name = "enable-authentication", havingValue = "true")
  public OwncloudUserDetailsService owncloudUserDetailsService(
      RestTemplateBuilder builder,
      AbstractOwncloudServiceImpl.Properties properties,
      MappingJackson2XmlHttpMessageConverter messageConverter) {
    return new OwncloudUserDetailsService(builder, properties, messageConverter);
  }

}
