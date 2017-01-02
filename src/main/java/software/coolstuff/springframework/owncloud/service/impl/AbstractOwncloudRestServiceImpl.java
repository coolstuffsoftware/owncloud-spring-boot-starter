/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.support.MessageSourceAccessor;
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
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
abstract class AbstractOwncloudRestServiceImpl implements OwncloudRestService {

  final static String DEFAULT_PATH = "/ocs/v1.php";

  private final RestTemplateBuilder restTemplateBuilder;
  private final boolean addBasicAuthentication;
  private final ResponseErrorHandler responseErrorHandler;

  protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private RestTemplate restTemplate;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter;

  @Autowired
  private FormHttpMessageConverter formHttpMessageConverter;

  protected AbstractOwncloudRestServiceImpl(RestTemplateBuilder builder) {
    this(builder, true);
  }

  protected AbstractOwncloudRestServiceImpl(RestTemplateBuilder builder, boolean addBasicAuthentication) {
    this(builder, addBasicAuthentication, new DefaultOwncloudResponseErrorHandler(SpringSecurityMessageSource.getAccessor()));
  }

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    URL locationURL = checkAndConvertLocation(properties.getLocation());
    configureRestTemplate(locationURL);
  }

  protected URL checkAndConvertLocation(String location) throws MalformedURLException {
    Validate.notBlank(location);
    log.debug("Check if the Location {} is a valid URL", location);
    URL url = new URL(location);
    log.debug("Check if the Location {} either start with http or https", location);
    if (isNotValidProtocol(url)) {
      final String exceptionMessage = "Invalid Protocol " + url.getProtocol() + ". Only http or https are allowed";
      log.error(exceptionMessage);
      throw new IllegalArgumentException(exceptionMessage);
    }
    return url;
  }

  private boolean isNotValidProtocol(URL url) {
    return !StringUtils.equals(url.getProtocol(), "http") && !StringUtils.equals(url.getProtocol(), "https");
  }

  private void configureRestTemplate(URL locationURL) throws MalformedURLException {
    log.debug("Extract the Root-URI from URL {}", locationURL);
    String rootURI = locationURL.toString();
    if (StringUtils.isBlank(locationURL.getPath()) || "/".equals(locationURL.getPath())) {
      rootURI = URI.create(locationURL.toString() + DEFAULT_PATH).toString();
    }

    if (addBasicAuthentication && StringUtils.isNotBlank(properties.getUsername())) {
      log.debug("Create the REST-Template to URI {} with the administrative User {}", rootURI, properties.getUsername());
      restTemplate = restTemplateBuilder
          .basicAuthorization(properties.getUsername(), properties.getPassword())
          .messageConverters(mappingJackson2XmlHttpMessageConverter)
          .additionalMessageConverters(formHttpMessageConverter)
          .errorHandler(responseErrorHandler)
          .rootUri(rootURI)
          .build();
    } else {
      log.debug("Create the REST-Template to URI {} to be used with the authenticated User", rootURI);
      restTemplate = restTemplateBuilder
          .messageConverters(mappingJackson2XmlHttpMessageConverter)
          .additionalMessageConverters(formHttpMessageConverter)
          .errorHandler(responseErrorHandler)
          .rootUri(rootURI)
          .build();
    }

    Validate.notNull(restTemplate);
  }

  @Override
  final public RestTemplate getRestTemplate() {
    return restTemplate;
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

  protected boolean isUseAdministratorCredentials() {
    return StringUtils.isNotBlank(properties.getUsername());
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
      Object... urlVariables)
      throws OwncloudStatusException {
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

}
