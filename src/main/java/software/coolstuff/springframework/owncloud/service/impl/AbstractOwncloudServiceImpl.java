package software.coolstuff.springframework.owncloud.service.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;

import lombok.EqualsAndHashCode;
import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthentication;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

abstract class AbstractOwncloudServiceImpl implements InitializingBean {

  final static String DEFAULT_PATH = "/ocs/v1.php";

  private final RestTemplateBuilder restTemplateBuilder;
  private final boolean addBasicAuthentication;

  private RestTemplate restTemplate;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder) {
    this(builder, true);
  }

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, boolean addBasicAuthentication) {
    this.restTemplateBuilder = builder;
    this.addBasicAuthentication = addBasicAuthentication;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Validate.notBlank(properties.getUrl());

    if (OwncloudResourceService.isNoResource(properties.getUrl())) {
      configureRestTemplate();
    } else {
      Validate.notNull(resourceService);
    }
  }

  private void configureRestTemplate() throws MalformedURLException {
    URL url = new URL(properties.getUrl());

    String rootURI = url.toString();
    if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
      rootURI = URI.create(url.toString() + DEFAULT_PATH).toString();
    }

    if (addBasicAuthentication && properties.isAuthenticateWithAdministrator()) {
      restTemplate = restTemplateBuilder
          .basicAuthorization(properties.getUsername(), properties.getPassword())
          .messageConverters(messageConverter)
          .errorHandler(new DefaultOwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    } else {
      restTemplate = restTemplateBuilder
          .messageConverters(messageConverter)
          .errorHandler(new DefaultOwncloudResponseErrorHandler())
          .rootUri(rootURI)
          .build();
    }

    Validate.notNull(restTemplate);
  }

  final RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected boolean isAuthenticateWithAdministrator() {
    return properties.isAuthenticateWithAdministrator();
  }

  protected HttpHeaders prepareHeaderWithBasicAuthorization(String username, String password) {
    Validate.notBlank(username);

    String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    return headers;
  }

  protected HttpHeaders prepareHeadersWithBasicAuthorization() {
    if (properties.isAuthenticateWithAdministrator()) {
      return prepareHeaderWithBasicAuthorization(properties.getUsername(), properties.getPassword());
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof OwncloudAuthentication)) {
      throw new OwncloudInvalidAuthentication(authentication);
    }

    return prepareHeaderWithBasicAuthorization(authentication.getName(), (String) authentication.getCredentials());
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

  protected OwncloudUserDetails createUserDetails(
      String username,
      OcsUserInformation userInformation,
      OcsGroups groups) {
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(username)
        .enabled(userInformation.getData().isEnabled())
        .displayName(userInformation.getData().getDisplayname())
        .email(userInformation.getData().getEmail())
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();

    if (groups != null) {
      List<GrantedAuthority> authorities = new ArrayList<>();
      for (String group : groups.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group));
      }

      if (grantedAuthoritiesMapper != null) {
        userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(authorities));
      } else {
        userDetails.setAuthorities(authorities);
      }
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
