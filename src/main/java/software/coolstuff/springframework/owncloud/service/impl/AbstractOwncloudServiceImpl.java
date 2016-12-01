package software.coolstuff.springframework.owncloud.service.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;

abstract class AbstractOwncloudServiceImpl implements InitializingBean {

  final static String DEFAULT_PATH = "/ocs/v1.php";

  private final RestTemplateBuilder restTemplateBuilder;
  private final boolean addBasicAuthentication;
  private final ResponseErrorHandler responseErrorHandler;

  private RestTemplate restTemplate;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter;

  @Autowired
  private FormHttpMessageConverter formHttpMessageConverter;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder) {
    this(builder, true);
  }

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, boolean addBasicAuthentication) {
    this(builder, addBasicAuthentication, new DefaultOwncloudResponseErrorHandler());
  }

  protected AbstractOwncloudServiceImpl(RestTemplateBuilder builder, boolean addBasicAuthentication, ResponseErrorHandler responseErrorHandler) {
    this.restTemplateBuilder = builder;
    this.addBasicAuthentication = addBasicAuthentication;
    this.responseErrorHandler = responseErrorHandler;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Validate.notBlank(properties.getLocation());

    if (OwncloudResourceService.isNoResource(properties.getLocation())) {
      configureRestTemplate();
    } else {
      Validate.notNull(resourceService);
    }
  }

  private void configureRestTemplate() throws MalformedURLException {
    URL url = new URL(properties.getLocation());

    String rootURI = url.toString();
    if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
      rootURI = URI.create(url.toString() + DEFAULT_PATH).toString();
    }

    if (addBasicAuthentication && StringUtils.isNotBlank(properties.getUsername())) {
      restTemplate = restTemplateBuilder
          .basicAuthorization(properties.getUsername(), properties.getPassword())
          .messageConverters(mappingJackson2XmlHttpMessageConverter)
          .additionalMessageConverters(formHttpMessageConverter)
          .errorHandler(responseErrorHandler)
          .rootUri(rootURI)
          .build();
    } else {
      restTemplate = restTemplateBuilder
          .messageConverters(mappingJackson2XmlHttpMessageConverter)
          .additionalMessageConverters(formHttpMessageConverter)
          .errorHandler(responseErrorHandler)
          .rootUri(rootURI)
          .build();
    }

    Validate.notNull(restTemplate);
  }

  final RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected final boolean isRestAvailable() {
    return restTemplate != null;
  }

  protected final boolean isRestNotAvailable() {
    return !isRestAvailable();
  }

  protected boolean isUseAdministratorCredentials() {
    return StringUtils.isNotBlank(properties.getUsername());
  }

  protected HttpHeaders prepareHeaderWithBasicAuthorization(String username, String password) {
    Validate.notBlank(username);

    String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    return headers;
  }

  protected HttpHeaders prepareHeadersWithBasicAuthorization() {
    if (isUseAdministratorCredentials()) {
      if (addBasicAuthentication) {
        return new HttpHeaders();
      }
      return prepareHeaderWithBasicAuthorization(properties.getUsername(), properties.getPassword());
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof OwncloudAuthentication) && !(authentication instanceof UsernamePasswordAuthenticationToken)) {
      throw new OwncloudInvalidAuthenticationObjectException(authentication);
    }

    return prepareHeaderWithBasicAuthorization(authentication.getName(), (String) authentication.getCredentials());
  }

  protected HttpEntity<String> emptyEntity(String username, String password) {
    return new HttpEntity<>(prepareHeaderWithBasicAuthorization(username, password));
  }

  protected HttpEntity<String> emptyEntity() {
    return new HttpEntity<>(prepareHeadersWithBasicAuthorization());
  }

  protected HttpEntity<MultiValueMap<String, String>> multiValuedEntity(Map<String, List<String>> data) {
    return new HttpEntity<>(new LinkedMultiValueMap<>(data), prepareHeadersWithBasicAuthorization());
  }

  protected <T extends AbstractOcs, ENTITY> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<ENTITY> httpEntity,
      Class<T> clazz,
      Object... urlVariables) throws OwncloudStatusException {
    return exchange(url, method, httpEntity, clazz, this::checkFailure, urlVariables);
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
    statusChecker.checkForFailure(url, result.getMeta());
    return result;
  }

  protected void checkFailure(String uri, AbstractOcs.Meta metaInformation) throws OwncloudStatusException {
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

    if (isGroupAvailable(groups)) {
      List<GrantedAuthority> authorities = new ArrayList<>();
      for (OcsGroups.Groups.Group group : groups.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group.getGroup()));
      }

      if (grantedAuthoritiesMapper != null) {
        userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(authorities));
      } else {
        userDetails.setAuthorities(authorities);
      }
    } else {
      userDetails.setAuthorities(new ArrayList<>());
    }

    return userDetails;
  }

  private boolean isGroupAvailable(OcsGroups groups) {
    return groups != null && groups.getData() != null && groups.getData().getGroups() != null;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  protected static class OcsVoid extends AbstractOcs {

    private String data;
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

      @lombok.Data
      @AllArgsConstructor
      protected static class Group {

        private String group;
      }

      @XmlElementWrapper
      private List<Group> groups;
    }

    private Groups data;
  }
}
