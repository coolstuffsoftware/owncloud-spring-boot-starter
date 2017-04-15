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

import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Base64.Encoder;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@ActiveProfiles("REST-RESOURCE-SERVICE")
public class OwncloudRestResourceServiceTest extends AbstractOwncloudResourceServiceTest implements OwncloudRestServiceTest {

  private final static String DEFAULT_PATH = "/remote.php/dav/files/{username}";

  @MockBean
  private SardineCacheLoader sardineCacheLoader;

  @MockBean
  private Sardine sardine;

  @Autowired
  private OwncloudResourceService resourceService;

  @MockBean
  private OwncloudRestUserQueryService userQueryService;

  @Autowired
  private OwncloudRestProperties properties;

  private MockRestServiceServer mockServer;

  @Before
  public void setUp() throws Exception {
    Mockito
        .when(sardineCacheLoader.load(Mockito.anyString()))
        .thenReturn(sardine);
    mockServer = MockRestServiceServer.createServer(owncloudService().getRestTemplate());
  }

  @After
  public void testDown() throws Exception {
    mockServer.verify();
  }

  @Override
  public OwncloudRestService owncloudService() {
    return (OwncloudRestService) resourceService;
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
          owncloudResource instanceof OwncloudFileResource ? ((OwncloudFileResource) owncloudResource).getContentLength() : null,
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
    return ((OwncloudResolveRootUriService) resourceService).getResolvedRootUri(username);
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
        .thenThrow(new SardineException("Resource not found", HttpStatus.NOT_FOUND.value(), "Resource not found"));
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
        .thenThrow(new SardineException("Resource not found", HttpStatus.NOT_FOUND.value(), "Resource not found"));
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
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andRespond(withSuccess(owncloudFileResource.getTestFileContent(), owncloudFileResource.getMediaType()));
  }

  private RequestMatcher requestToWithPrefix(URI href) throws MalformedURLException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkRestLocation();
    URI uri = URI.create(UriComponentsBuilder.fromHttpUrl(properties.getLocation())
        .path(StringUtils.replace(DEFAULT_PATH, "{username}", authentication.getName()))
        .path(href.getPath())
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
  protected void prepare_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
  }

  @Override
  protected void prepare_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(owncloudFileResource.getMediaType()))
        .andExpect(content().string(owncloudFileResource.getTestFileContent()))
        .andRespond(withSuccess());
  }

  @Override
  protected void prepare_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(owncloudFileResource.getMediaType()))
        .andExpect(content().bytes(new byte[] { 1 }))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
  }

  @Override
  protected void prepare_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Mockito
        .when(sardine.list(getResourcePath(href), 0))
        .thenThrow(new SardineException("No File", HttpStatus.NOT_FOUND.value(), null));
    mockServer
        .expect(requestToWithPrefix(href))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(mediaType))
        .andExpect(content().string(testFileContent))
        .andRespond(withSuccess());
  }

  @Override
  protected void check_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Mockito.verify(sardine).list(getResourcePath(href), 0);
  }

  @Override
  protected void prepare_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
    List<DavResource> davResources = Lists.newArrayList(
        createDavResourceFrom(
            OwncloudTestResourceImpl.builder()
                .href(href)
                .mediaType(OwncloudUtils.getDirectoryMediaType())
                .build(),
            Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(href), 0))
        .thenReturn(davResources);
  }

  @Override
  protected void check_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
    Mockito.verify(sardine).list(getResourcePath(href), 0);
  }

  @Override
  protected void prepare_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    List<DavResource> davResources = Lists.newArrayList(
        createDavResourceFrom(
            OwncloudTestFileResourceImpl.fileBuilder()
                .owncloudResource(
                    OwncloudTestResourceImpl.builder()
                        .href(href)
                        .mediaType(mediaType)
                        .build())
                .testFileContent(testFileContent)
                .build(),
            Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(href)))
        .thenReturn(davResources);
    mockServer
        .expect(requestToWithPrefix(href))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(mediaType))
        .andExpect(content().string(testFileContent))
        .andRespond(withSuccess());
  }

  @Override
  protected void check_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Mockito.verify(sardine).list(getResourcePath(href), 0);
  }

  @Override
  protected void prepare_deleteFile_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withNoContent());
  }

  @Override
  protected void prepare_deleteFile_NOK_FileNotExists(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
  }

  @Override
  protected void prepare_deleteFile_NOK_OtherError(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudFileResource.getHref()))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
  }

  @Override
  protected void prepare_deleteDirectory_OK(OwncloudTestResourceImpl owncloudResource) throws Exception {
    mockServer
        .expect(requestToWithPrefix(owncloudResource.getHref()))
        .andExpect(method(HttpMethod.DELETE))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withNoContent());
  }

  @Override
  protected void prepare_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    ArrayList<DavResource> result = Lists.newArrayList(createDavResourceFrom(expectedResource, Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(expectedResource.getHref()), 0))
        .then(new Answer<List<DavResource>>() {
          private int count = 0;

          @Override
          public List<DavResource> answer(InvocationOnMock invocation) throws Throwable {
            if (count++ == 0) {
              throw new SardineException("not Found", HttpStatus.NOT_FOUND.value(), "not Found");
            }
            return result;
          }
        });
  }

  @Override
  protected void check_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    Mockito.verify(sardine)
        .createDirectory(getResourcePath(expectedResource.getHref()));
  }

  @Override
  protected void prepare_createDirectory_NOK_AlreadyExistsAsFile(URI uri) throws Exception {
    OwncloudTestFileResourceImpl existingResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(OwncloudTestResourceImpl.builder()
            .href(uri)
            .mediaType(MediaType.TEXT_PLAIN)
            .build())
        .testFileContent("No matter what content is in the existing Resource")
        .build();
    ArrayList<DavResource> result = Lists.newArrayList(createDavResourceFrom(existingResource, Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(uri), 0))
        .thenReturn(result);
  }

  @Override
  protected void check_createDirectory_NOK_AlreadyExistsAsFile(URI uri) throws Exception {
    Mockito.verify(sardine)
        .list(getResourcePath(uri), 0);
    Mockito.verify(sardine, Mockito.never())
        .createDirectory(getResourcePath(uri));
  }

  @Override
  protected void prepare_createDirectory_OK_AlreadyExistsAsDirectory(OwncloudTestResourceImpl expected) throws Exception {
    ArrayList<DavResource> result = Lists.newArrayList(createDavResourceFrom(expected, Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(expected.getHref()), 0))
        .thenReturn(result);
  }

  @Override
  protected void check_createDirectory_OK_AlreadyExistsAsDirectory(OwncloudTestResourceImpl expected) throws Exception {
    Mockito.verify(sardine)
        .list(getResourcePath(expected.getHref()), 0);
    Mockito.verify(sardine, Mockito.never())
        .createDirectory(getResourcePath(expected.getHref()));
  }

  @Override
  protected void prepare_getOutputStram_NOK_FileTooBig(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Mockito
        .when(sardine.list(getResourcePath(href), 0))
        .thenThrow(new SardineException("No File", HttpStatus.NOT_FOUND.value(), null));
    mockServer
        .expect(requestToWithPrefix(href))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(mediaType))
        .andExpect(content().string(testFileContent))
        .andRespond(withStatus(HttpStatus.INSUFFICIENT_STORAGE));
  }

  @Override
  protected void check_getOutputStram_NOK_FileTooBig(URI uri) throws Exception {
    Mockito.verify(sardine).list(getResourcePath(uri), 0);
  }

  @Override
  protected void prepare_getQuota_NoFiles(OwncloudQuota expected) throws Exception {
    OwncloudRestQuotaImpl restQuota = OwncloudRestQuotaImpl.builder()
        .username(expected.getUsername())
        .free(expected.getFree())
        .relative(expected.getRelative())
        .total(expected.getTotal())
        .used(expected.getUsed())
        .build();
    Mockito
        .when(userQueryService.getQuota(expected.getUsername()))
        .thenReturn(restQuota);
  }

  @Override
  protected void prepare_getQuota_OneFile(URI uri, MediaType mediaType, String testFileContent, OwncloudQuota expectedFirst, OwncloudQuota expectedSecond) throws Exception {
    mockServer
        .expect(requestToWithPrefix(uri))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(header(HttpHeaders.CONNECTION, "keep-alive"))
        .andExpect(content().contentType(mediaType))
        .andExpect(content().string(testFileContent))
        .andRespond(withSuccess());

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Mockito
        .when(userQueryService.getQuota(authentication.getName()))
        .then(new Answer<OwncloudQuota>() {
          private int count = 0;

          @Override
          public OwncloudQuota answer(InvocationOnMock invocation) throws Throwable {
            if (count++ == 0) {
              return OwncloudRestQuotaImpl.builder()
                  .username(expectedFirst.getUsername())
                  .free(expectedFirst.getFree())
                  .relative(expectedFirst.getRelative())
                  .total(expectedFirst.getTotal())
                  .used(expectedFirst.getUsed())
                  .build();
            }
            return OwncloudRestQuotaImpl.builder()
                .username(expectedSecond.getUsername())
                .free(expectedSecond.getFree())
                .relative(expectedSecond.getRelative())
                .total(expectedSecond.getTotal())
                .used(expectedSecond.getUsed())
                .build();
          }
        });
  }

  @Test(expected = OwncloudRestResourceException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_createDirectory_NOK_NotAllowed() throws Exception {
    URI uri = URI.create("/testDirectory");
    Mockito
        .doThrow(new SardineException("not allowed", HttpStatus.METHOD_NOT_ALLOWED.value(), "Method not allowed"))
        .when(sardine).createDirectory(getResourcePath(uri));

    resourceService.createDirectory(uri);
  }
}
