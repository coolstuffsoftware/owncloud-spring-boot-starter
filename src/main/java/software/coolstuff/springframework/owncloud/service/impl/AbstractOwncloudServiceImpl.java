package software.coolstuff.springframework.owncloud.service.impl;

import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
    this(builder, properties, true, messageConverter);
  }

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, Properties properties, boolean addBasicAuthentication, MappingJackson2XmlHttpMessageConverter messageConverter) {
    if (properties.getUrl() == null) {
      throw new IllegalArgumentException("URL of the OwnCloud-Service must be configured");
    }

    String rootURI = properties.getUrl().toString();
    if (StringUtils.isEmpty(properties.getUrl().getPath()) || "/".equals(properties.getUrl().getPath())) {
      rootURI = URI.create(properties.getUrl().toString() + DEFAULT_PATH).toString();
    }

    if (addBasicAuthentication) {
      restTemplate = builder
          .basicAuthorization(properties.getUsername(), properties.getPassword())
          .messageConverters(messageConverter)
          .errorHandler(new OwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    } else {
      restTemplate = builder
          .messageConverters(messageConverter)
          .errorHandler(new OwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    }
  }

  protected <T extends AbstractOcs> T getForObjectWithErrorCheck(String url, Class<T> clazz, Object... urlVariables) {
    T result = restTemplate.getForObject(url, clazz, urlVariables);
    checkForFailure(url, result.getMeta());
    return result;
  }

  private void checkForFailure(String uri, AbstractOcs.Meta metaInformation) {
    if ("ok".equals(metaInformation.getStatus())) {
      return;
    }

    switch (metaInformation.getStatuscode()) {
      case 100:
        return;
      case 997:
        throw new AccessDeniedException("Not Authorized to access Resource " + uri);
      default:
        throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
    }
  }

  @lombok.Data
  protected static abstract class AbstractOcs {

    @lombok.Data
    private static class Meta {
      private String status;
      private int statuscode;
      private String message;
    }

    private Meta meta;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  protected static class OcsUsers extends AbstractOcs {

    @lombok.Data
    private static class Users {
      @XmlElementWrapper
      private List<String> users;
    }

    private Users data;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  protected static class OcsUserInformation extends AbstractOcs {

    @lombok.Data
    private static class User {

      @lombok.Data
      private static class Quota {
        private Long free;
        private Long used;
        private Long total;
        private Float relative;
      }

      private boolean enabled;
      private Quota quota;
      private String email;
      private String displayname;
    }

    private User data;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  protected static class OcsGroups extends AbstractOcs {

    @lombok.Data
    private static class Groups {
      @XmlElementWrapper
      private List<String> groups;
    }

    private Groups data;
  }
}
