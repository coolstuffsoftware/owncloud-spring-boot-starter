package software.coolstuff.springframework.owncloud.service.impl;

import java.net.URI;
import java.net.URL;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import lombok.Data;

@EnableConfigurationProperties(AbstractOwncloudServiceImpl.Properties.class)
public abstract class AbstractOwncloudServiceImpl {

  private final static String DEFAULT_PATH = "/ocs/v1.php/cloud";

  @Data
  @ConfigurationProperties(prefix = "owncloud")
  public static class Properties {
    private URL url;
    private boolean enableAuthentication = false;
    private String username;
    private String password;
  }

  private final RestTemplate restTemplate;

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    if (properties.getUrl() == null) {
      throw new IllegalArgumentException("URL of the OwnCloud-Service must be configured");
    }

    String rootURI = properties.getUrl().toString();
    if (StringUtils.isEmpty(properties.getUrl().getPath()) || "/".equals(properties.getUrl().getPath())) {
      rootURI = URI.create(properties.getUrl().toString() + DEFAULT_PATH).toString();
    }

    restTemplate = builder
        .basicAuthorization(properties.getUsername(), properties.getPassword())
        .messageConverters(messageConverter)
        .errorHandler(new OwncloudResponseErrorHandler())
        .rootUri(rootURI)
        .build();
  }

  protected RestTemplate getRestTemplate() {
    return restTemplate;
  }
}
