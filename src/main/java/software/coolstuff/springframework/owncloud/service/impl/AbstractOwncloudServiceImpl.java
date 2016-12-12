package software.coolstuff.springframework.owncloud.service.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

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
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

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
    headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
    return headers;
  }

  protected HttpHeaders prepareHeadersWithBasicAuthorization() {
    if (isUseAdministratorCredentials()) {
      if (addBasicAuthentication) {
        // Authentication Header will be added by RestTemplate.basicAuthorization()
        // so we don't need to add any Authentication Headers
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
    HttpHeaders headers = prepareHeadersWithBasicAuthorization();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(new LinkedMultiValueMap<>(data), headers);
  }

  protected <T extends Ocs, ENTITY> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<ENTITY> httpEntity,
      Class<T> clazz,
      Object... urlVariables) throws OwncloudStatusException {
    return exchange(url, method, httpEntity, clazz, this::checkFailure, urlVariables);
  }

  protected <T extends Ocs, ENTITY> T exchange(
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

  protected void checkFailure(String uri, Ocs.Meta meta) throws OwncloudStatusException {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    switch (meta.getStatuscode()) {
      case 997:
        throw new AccessDeniedException("Not Authorized to access Resource " + uri);
      case 998:
        throw new UsernameNotFoundException(meta.getMessage());
      default:
        throw new IllegalStateException("Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
    }
  }

  protected OwncloudUserDetails createUserDetails(String username, Ocs.User user, Ocs.Groups groupsFromBackend) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (isGroupAvailable(groupsFromBackend)) {
      for (Ocs.Groups.Data.Group group : groupsFromBackend.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group.getGroup()));
        groups.add(group.getGroup());
      }
    }

    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(username)
        .enabled(user.getData().isEnabled())
        .displayName(user.getData().getDisplayname())
        .email(user.getData().getEmail())
        .groups(groups)
        .authorities(authorities)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
    if (owncloudGrantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), authorities));
    } else if (grantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(authorities));
    }

    return userDetails;
  }

  private boolean isGroupAvailable(Ocs.Groups groups) {
    return groups != null && groups.getData() != null && groups.getData().getGroups() != null;
  }
}
