package software.coolstuff.springframework.owncloud.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudUnauthorizedException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudService;

@EnableConfigurationProperties(OwncloudServiceImpl.Properties.class)
@Slf4j
public class OwncloudServiceImpl implements OwncloudService {

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

  public OwncloudServiceImpl(RestTemplateBuilder builder, Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
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

  @Slf4j
  private static class OwncloudResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      try {
        HttpStatus statusCode = response.getStatusCode();
        if (HttpStatus.UNAUTHORIZED.compareTo(statusCode) == 0) {
          throw new OwncloudUnauthorizedException(statusCode.getReasonPhrase());
        }
        super.handleError(response);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        throw e;
      }
    }

  }
}
