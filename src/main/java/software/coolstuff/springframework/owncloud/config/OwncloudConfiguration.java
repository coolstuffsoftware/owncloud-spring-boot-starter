package software.coolstuff.springframework.owncloud.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.coolstuff.springframework.owncloud.service.api.OwncloudService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAuthenticationProvider;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudServiceImpl;

@Configuration
@ConditionalOnProperty(prefix = "owncloud", name = "url")
public class OwncloudConfiguration {

  @Bean
  public OwncloudService owncloudService(RestTemplateBuilder restTemplateBuilder, OwncloudServiceImpl.Properties properties) {
    return null;
  }

  @Bean
  @ConditionalOnProperty(prefix = "owncloud", name = "enable-authentication", havingValue = "true")
  public OwncloudAuthenticationProvider owncloudAuthenticationProvider(RestTemplateBuilder restTemplateBuilder, OwncloudServiceImpl.Properties properties) {
    return null;
  }
}
