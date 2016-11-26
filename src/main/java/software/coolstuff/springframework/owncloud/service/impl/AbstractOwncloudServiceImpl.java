package software.coolstuff.springframework.owncloud.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;

import lombok.EqualsAndHashCode;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public abstract class AbstractOwncloudServiceImpl {

  final static String DEFAULT_PATH = "/ocs/v1.php/cloud";

  private final RestTemplate restTemplate;
  private final OwncloudProperties properties;

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, OwncloudProperties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    this(builder, properties, true, messageConverter);
  }

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, OwncloudProperties properties, boolean addBasicAuthentication, MappingJackson2XmlHttpMessageConverter messageConverter) {
    this.properties = properties;

    if (properties.getUrl() == null) {
      throw new IllegalArgumentException("URL of the OwnCloud-Service must be configured");
    }

    String rootURI = properties.getUrl().toString();
    if (StringUtils.isBlank(properties.getUrl().getPath()) || "/".equals(properties.getUrl().getPath())) {
      rootURI = URI.create(properties.getUrl().toString() + DEFAULT_PATH).toString();
    }

    if (addBasicAuthentication) {
      restTemplate = builder
          .basicAuthorization(properties.getUsername(), properties.getPassword())
          .messageConverters(messageConverter)
          .errorHandler(new DefaultOwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    } else {
      restTemplate = builder
          .messageConverters(messageConverter)
          .errorHandler(new DefaultOwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    }
  }

  final RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected <T extends AbstractOcs> T getForObject(
      String url,
      Class<T> clazz,
      Object... urlVariables) throws OwncloudStatusException {
    return getForObject(url, clazz, this::checkForFailure, urlVariables);
  }

  protected <T extends AbstractOcs> T getForObject(
      String url,
      Class<T> clazz,
      OwncloudResponseStatusChecker statusChecker,
      Object... urlVariables) throws OwncloudStatusException {
    T result = restTemplate.getForObject(url, clazz, urlVariables);
    statusChecker.checkForFailure(url, result.getMeta());
    return result;
  }

  protected <T extends AbstractOcs, ENTITY> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<ENTITY> httpEntity,
      Class<T> clazz,
      Object... urlVariables) throws OwncloudStatusException {
    return exchange(url, method, httpEntity, clazz, this::checkForFailure, urlVariables);
  }

  protected <T extends AbstractOcs, ENTITY> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<ENTITY> httpEntity,
      Class<T> clazz,
      OwncloudResponseStatusChecker statusChecker,
      Object... urlVariables) throws OwncloudStatusException {
    ResponseEntity<T> response = restTemplate.exchange(url, method, httpEntity, clazz, urlVariables);
    T result = response.getBody();
    checkForFailure(url, result.getMeta());
    return result;
  }

  protected void checkForFailure(String uri, AbstractOcs.Meta metaInformation) throws OwncloudStatusException {
    if ("ok".equals(metaInformation.getStatus())) {
      return;
    }

    switch (metaInformation.getStatuscode()) {
      case 100:
        return;
      case 997:
        throw new AccessDeniedException("Not Authorized to access Resource " + uri);
      case 998:
        throw new UsernameNotFoundException(metaInformation.getMessage());
      default:
        throw new IllegalStateException("Unknown Error Code " + metaInformation.getStatuscode() + ". Reason: " + metaInformation.getMessage());
    }
  }

  protected OwncloudUserDetails createUserDetails(String username, OcsUserInformation userInformation, OcsGroups groups) {
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(username)
        .enabled(userInformation.getData().isEnabled())
        .displayName(userInformation.getData().getDisplayname())
        .email(userInformation.getData().getEmail())
        .build();

    if (groups != null) {
      List<SimpleGrantedAuthority> authorities = new ArrayList<>();
      for (String group : groups.getData().getGroups()) {
        if (StringUtils.isBlank(properties.getRolePrefix()) || StringUtils.startsWith(group, properties.getRolePrefix())) {
          authorities.add(new SimpleGrantedAuthority(group));
        } else {
          authorities.add(new SimpleGrantedAuthority(properties.getRolePrefix() + group));
        }
      }
      userDetails.setAuthorities(authorities);
    }

    return userDetails;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  protected static class OcsUsers extends AbstractOcs {

    @lombok.Data
    protected static class Users {
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
    protected static class User {

      @lombok.Data
      protected static class Quota {
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
    protected static class Groups {
      @XmlElementWrapper
      private List<String> groups;
    }

    private Groups data;
  }
}
