/*
   Copyright (C) 2017 by the original Authors.

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

@ActiveProfiles("REST-RESOURCE-SERVICE")
public class OwncloudRestResourceServiceTest extends AbstractOwncloudResourceServiceTest implements OwncloudRestServiceTest {

  private final static String DEFAULT_PATH = "/remote.php/dav/files/{username}";

  @MockBean
  private SardineCacheLoader sardineCacheLoader;

  @MockBean
  private Sardine sardine;

  @Autowired
  private OwncloudResourceService resourceService;

  @Autowired
  private OwncloudRestProperties properties;

  private MockRestServiceServer mockServer;

  @Before
  public void setUp() throws Exception {
    Mockito
        .when(sardineCacheLoader.load(Mockito.anyString()))
        .thenReturn(sardine);
    assertThat(resourceService.getClass()).isAssignableFrom(OwncloudRestResourceServiceImpl.class);
    mockServer = MockRestServiceServer.createServer(owncloudService().getRestTemplate());
  }

  @Override
  public OwncloudRestService owncloudService() {
    return (OwncloudRestService) resourceService;
  }

  @Override
  protected Class<? extends OwncloudResourceService> getImplementationClass() {
    return OwncloudRestResourceServiceImpl.class;
  }

  @Override
  protected OwncloudResource prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(OwncloudResource expected) throws Exception {
    return OwncloudRestResourceImpl.builder()
        .eTag(expected.getETag())
        .href(expected.getHref())
        .lastModifiedAt(expected.getLastModifiedAt())
        .mediaType(expected.getMediaType())
        .name(expected.getName())
        .build();
  }

  @Override
  protected void prepare_listRoot_OK(List<OwncloudTestResourceImpl> expectedOwncloudResources) throws Exception {
    List<DavResource> expectedDavResources = expectedOwncloudResources.stream()
        .map(owncloudResource -> createDavResourceFrom(owncloudResource, Locale.GERMAN))
        .collect(Collectors.toList());
    Mockito
        .when(sardine.list(getResourcePath()))
        .thenReturn(expectedDavResources);
  }

  private DavResource createDavResourceFrom(OwncloudTestResourceImpl owncloudResource, Locale locale) {
    URI prefixedHref = resolveAsFileURI(owncloudResource.getHref());
    String contentLanguage = Optional.ofNullable(locale)
        .map(loc -> loc.getLanguage())
        .orElse(null);
    String name = owncloudResource.getBackendName();
    String eTag = owncloudResource.getBackendETag();
    if (isRoot(owncloudResource.getHref())) {
      eTag = null;
    }
    try {
      return new OwncloudDavResource(
          prefixedHref.getPath(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getMediaType().toString(),
          (owncloudResource instanceof OwncloudFileResource ? ((OwncloudFileResource) owncloudResource).getContentLength() : null),
          eTag,
          name,
          contentLanguage);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("DavResource couldn't be built by OwncloudResource", e);
    }
  }

  private URI resolveAsFileURI(URI relativeTo) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return URI.create(
        UriComponentsBuilder.fromUri(getResolvedRootUri(authentication.getName()))
            .path(relativeTo.getPath())
            .toUriString());
  }

  private URI getResolvedRootUri(String username) {
    return ((OwncloudRestResourceServiceImpl) resourceService).getResolvedRootUri(username);
  }

  private boolean isRoot(URI searchPath) {
    return "/".equals(searchPath.getPath());
  }

  private static class OwncloudDavResource extends DavResource {

    public OwncloudDavResource(
        String href,
        Date created,
        Date modified,
        String contentType,
        Long contentLength,
        String eTag,
        String displayName,
        String contentLanguage) throws URISyntaxException {
      super(
          href,
          created,
          modified,
          contentType,
          contentLength,
          eTag,
          displayName,
          null,
          contentLanguage,
          null,
          null);
    }
  }

  private String getResourcePath() {
    return getResourcePath(null);
  }

  private String getResourcePath(URI searchPath) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return Optional.ofNullable(resolveAsDirectoryURI(searchPath, authentication.getName()))
        .map(uri -> uri.toString())
        .orElse(null);
  }

  private URI resolveAsDirectoryURI(URI relativeTo, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return resolvedRootUri;
    }
    return URI.create(
        UriComponentsBuilder.fromUri(resolvedRootUri)
            .path(relativeTo.getPath())
            .path("/")
            .toUriString());
  }

  @Override
  protected void prepare_list_OK(URI searchPath, List<OwncloudTestResourceImpl> expectedOwncloudResources) throws Exception {
    List<DavResource> expectedDavResources = expectedOwncloudResources.stream()
        .filter(owncloudResource -> !StringUtils.equals(owncloudResource.getName(), ".."))
        .map(owncloudResource -> createDavResourceFrom(owncloudResource, Locale.GERMAN))
        .collect(Collectors.toList());
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenReturn(expectedDavResources);

    if (isNotRoot(searchPath) && properties.getResourceService().isAddRelativeDownPath()) {
      expectedOwncloudResources.stream()
          .filter(owncloudResource -> StringUtils.equals(owncloudResource.getName(), ".."))
          .forEach(owncloudResource -> {
            try {
              DavResource superDavResource = createDavResourceFrom(owncloudResource, Locale.GERMAN);
              Mockito
                  .when(sardine.list(getResourcePath(URI.create("/directory/")), 0))
                  .thenReturn(Lists.newArrayList(superDavResource));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  private boolean isNotRoot(URI searchPath) {
    return !isRoot(searchPath);
  }

  @Override
  protected void prepare_list_NOK_FileNotFound(URI searchPath) throws Exception {
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenThrow(new SardineException("Resource not found", HttpStatus.SC_NOT_FOUND, "Resource not found"));
  }

  @Override
  protected void prepare_findFile_OK(URI searchPath, OwncloudTestFileResourceImpl expectedResource) throws Exception {
    List<DavResource> davResources = Lists.newArrayList(
        createDavResourceFrom(expectedResource, Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(searchPath), 0))
        .thenReturn(davResources);
  }

  @Override
  protected void prepare_findFile_NotExists(URI searchPath) throws Exception {
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenThrow(new SardineException("Resource not found", HttpStatus.SC_NOT_FOUND, "Resource not found"));
  }

  @Override
  protected void prepare_findRootDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    List<DavResource> davResources = Lists.newArrayList(
        createDavResourceFrom(expectedResource, Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(), 0))
        .thenReturn(davResources);
  }

  @Override
  protected void prepare_getInputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andRespond(withSuccess(owncloudFileResource.getTestFileContent(), owncloudFileResource.getMediaType()));
  }

  private RequestMatcher requestToWithPrefix(OwncloudResource owncloudResource) throws MalformedURLException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkRestLocation();
    URI uri = URI.create(UriComponentsBuilder.fromHttpUrl(properties.getLocation())
        .path(StringUtils.replace(DEFAULT_PATH, "{username}", authentication.getName()))
        .path(owncloudResource.getHref().getPath())
        .toUriString());
    return requestTo(uri);
  }

  private void checkRestLocation() {
    if (isResourceLocation()) {
      fail("The specified Location is not a REST Location");
    }
  }

  private boolean isResourceLocation() {
    return StringUtils.startsWith(properties.getLocation(), "file:") || StringUtils.startsWith(properties.getLocation(), "classpath:");
  }

  private String getBasicAuthorizationHeader() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Encoder base64Encoder = Base64.getEncoder();
    String encodedCredentials = base64Encoder.encodeToString((authentication.getName() + ':' + (String) authentication.getCredentials()).getBytes());
    return "Basic " + encodedCredentials;
  }

  @Override
  protected void check_getInputStream_OK() throws Exception {
    mockServer.verify();
  }

  @Override
  protected void prepare_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
  }
}
