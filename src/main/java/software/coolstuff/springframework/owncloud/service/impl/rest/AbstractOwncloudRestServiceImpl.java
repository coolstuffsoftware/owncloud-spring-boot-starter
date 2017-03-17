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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsMappingService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
abstract class AbstractOwncloudRestServiceImpl implements OwncloudRestService {

  private static final String DEFAULT_PATH = "/ocs/v1.php";

  private static final String AUTHORIZATION_METHOD_PREFIX = "Basic ";

  private final RestTemplateBuilder restTemplateBuilder;
  private final ResponseErrorHandler responseErrorHandler;

  protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private RestTemplate restTemplate;

  @Autowired
  private OwncloudRestProperties properties;

  @Autowired
  private OwncloudUserDetailsMappingService owncloudUserDetailsMappingService;

  protected AbstractOwncloudRestServiceImpl(RestTemplateBuilder builder) {
    this(builder, new DefaultOwncloudResponseErrorHandler(SpringSecurityMessageSource.getAccessor()));
  }

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    URL locationURL = OwncloudRestUtils.checkAndConvertLocation(properties.getLocation());
    configureRestTemplate(locationURL);
  }

  private void configureRestTemplate(URL locationURL) throws MalformedURLException {
    String rootUri = OwncloudRestUtils.appendDefaultPath(locationURL, DEFAULT_PATH);
    log.info("Create the REST-Template to URI {} to be used with the authenticated User", rootUri);
    restTemplate = restTemplateBuilder
        .additionalMessageConverters(new FormHttpMessageConverter())
        .errorHandler(responseErrorHandler)
        .rootUri(rootUri)
        .build();
    Validate.notNull(restTemplate);
  }

  @Override
  public final RestTemplate getRestTemplate() {
    return restTemplate;
  }

  protected HttpHeaders prepareHeadersWithBasicAuthorization() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (OwncloudUtils.isAuthenticationClassNotSupported(authentication.getClass())) {
      throw new OwncloudInvalidAuthenticationObjectException(authentication, UsernamePasswordAuthenticationToken.class);
    }
    return OwncloudRestUtils.addAuthorizationHeader(authentication);
  }

  protected String getLocation() {
    return properties.getLocation();
  }

  protected HttpEntity<String> emptyEntity(String username, String password) {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
    HttpHeaders headers = OwncloudRestUtils.addAuthorizationHeader(authenticationToken);
    return new HttpEntity<>(headers);
  }

  protected HttpEntity<String> emptyEntity() {
    return new HttpEntity<>(prepareHeadersWithBasicAuthorization());
  }

  protected HttpEntity<MultiValueMap<String, String>> multiValuedEntity(Map<String, List<String>> data) {
    HttpHeaders headers = prepareHeadersWithBasicAuthorization();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(new LinkedMultiValueMap<>(data), headers);
  }

  protected <T extends Ocs, E> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<E> httpEntity,
      Class<T> clazz,
      Object... urlVariables) {
    return exchange(url, method, httpEntity, clazz, this::checkFailure, urlVariables);
  }

  protected <T extends Ocs, E> T exchange(
      String url,
      HttpMethod method,
      HttpEntity<E> httpEntity,
      Class<T> clazz,
      OwncloudResponseStatusChecker statusChecker,
      Object... urlVariables) {
    log.trace("Exchange Data by a {} Request with URL {}. Requested Class of returned Data is {}", method, url, clazz);
    ResponseEntity<T> response = restTemplate.exchange(url, method, httpEntity, clazz, urlVariables);
    T result = response.getBody();
    log.trace("Returned Meta-Data: {}", result.getMeta());
    log.debug("Check the returned Meta-Data for Errors");
    String authorizationUser = getAuthorizationUserFromHeaders(httpEntity.getHeaders());
    statusChecker.checkForFailure(authorizationUser, url, result.getMeta());
    return result;
  }

  protected String getAuthorizationUserFromHeaders(HttpHeaders headers) {
    Validate.notNull(headers);

    List<String> authorizations = headers.get(HttpHeaders.AUTHORIZATION);
    if (CollectionUtils.isEmpty(authorizations)) {
      return null;
    }

    String encodedCredentials = authorizations.get(0);
    if (StringUtils.startsWith(encodedCredentials, AUTHORIZATION_METHOD_PREFIX)) {
      encodedCredentials = StringUtils.substring(encodedCredentials, AUTHORIZATION_METHOD_PREFIX.length());
    }
    final byte[] rawDecodedCredentials = Base64.getDecoder().decode(encodedCredentials.getBytes());
    final String decodedCredentials = new String(rawDecodedCredentials);
    if (!StringUtils.contains(decodedCredentials, ':')) {
      return null;
    }
    return StringUtils.split(decodedCredentials, ':')[0];
  }

  protected void checkFailure(String username, String uri, Ocs.Meta meta) throws OwncloudStatusException {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    String exceptionMessage;
    switch (meta.getStatuscode()) {
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", username, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      case 998:
        log.error("Error 998: {}", meta.getMessage());
        throw new UsernameNotFoundException(meta.getMessage());
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  protected OwncloudUserDetails convert(String username, Ocs.User user, Ocs.Groups groupsFromBackend) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (isAnyOwncloudGroupAvailable(groupsFromBackend)) {
      log.trace("Put {} Owncloud-Group(s) into the Authorities- and Group-List");
      for (Ocs.Groups.Data.Group owncloudGroup : groupsFromBackend.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(owncloudGroup.getGroup()));
        groups.add(owncloudGroup.getGroup());
      }
    }

    log.debug("Convert User {} from {} to {}", username, user.getClass(), OwncloudUserDetails.class);
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(username)
        .enabled(user.getData().isEnabled())
        .displayname(user.getData().getDisplayname())
        .email(user.getData().getEmail())
        .groups(groups)
        .authorities(authorities)
        .build();
    owncloudUserDetailsMappingService.mapGrantedAuthorities(userDetails);
    return userDetails;
  }

  private boolean isAnyOwncloudGroupAvailable(Ocs.Groups groups) {
    return groups != null && groups.getData() != null && groups.getData().getGroups() != null;
  }
}
