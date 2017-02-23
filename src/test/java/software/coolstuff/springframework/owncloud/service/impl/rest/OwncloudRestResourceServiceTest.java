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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@ActiveProfiles("REST-RESOURCE-SERVICE")
public class OwncloudRestResourceServiceTest extends AbstractOwncloudResourceServiceTest {

  @MockBean
  private SardineCacheLoader sardineCacheLoader;

  @MockBean
  private Sardine sardine;

  @Autowired
  private OwncloudResourceService resourceService;

  @Autowired
  private OwncloudRestProperties properties;

  @Before
  public void setUp() throws Exception {
    Mockito
        .when(sardineCacheLoader.load(Mockito.anyString()))
        .thenReturn(sardine);
  }

  @Override
  protected Class<? extends OwncloudResourceService> getImplementationClass() {
    return OwncloudRestResourceServiceImpl.class;
  }

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_listRoot_OK() throws Exception {
    List<DavResource> expectedResources = Lists.newArrayList(
        createDavResource("/", OwncloudUtils.getDirectoryMediaType(), null, "/", null),
        createDavResource("/resource1", MediaType.APPLICATION_PDF, Long.valueOf(1234), "resource1", Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath()))
        .thenReturn(expectedResources);

    List<OwncloudResource> resources = resourceService.listRoot();
    compare(resources, expectedResources, null);
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

  private URI getResolvedRootUri(String username) {
    return ((OwncloudRestResourceServiceImpl) resourceService).getResolvedRootUri(username);
  }

  private DavResource createDavResource(
      String href,
      MediaType mediaType,
      Long contentLength,
      String displayName,
      Locale locale) throws URISyntaxException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI prefixedHref = resolveAsFileURI(URI.create(href), authentication.getName());
    String contentLanguage = Optional.ofNullable(locale)
        .map(loc -> loc.getLanguage())
        .orElse(null);
    return new OwncloudDavResource(
        prefixedHref.getPath(),
        new Date(),
        new Date(),
        mediaType.toString(),
        contentLength,
        UUID.randomUUID().toString(),
        displayName,
        contentLanguage);
  }

  private URI resolveAsFileURI(URI relativeTo, String username) {
    return URI.create(
        UriComponentsBuilder.fromUri(getResolvedRootUri(username))
            .path(relativeTo.getPath())
            .toUriString());
  }

  private void compare(List<OwncloudResource> actualResources, List<DavResource> expectedResources, DavResource superResource) {
    compare(null, actualResources, expectedResources, superResource);
  }

  private void compare(URI searchPath, List<OwncloudResource> actualResources, List<DavResource> expectedResources, DavResource expectedSuperResource) {
    assertThat(actualResources).isNotNull();
    assertThat(actualResources).isNotEmpty();

    Map<URI, DavResource> mappedExpectedResources = new HashMap<>();
    for (DavResource davResource : expectedResources) {
      mappedExpectedResources.put(davResource.getHref(), davResource);
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudResource actualSuperResource = null;
    List<OwncloudResource> unexpectedResources = new ArrayList<>();
    for (OwncloudResource actualResource : actualResources) {
      URI uri = resolveAsFileURI(actualResource.getHref(), authentication.getName());
      uri = URI.create(uri.getPath());
      if (!mappedExpectedResources.containsKey(uri)) {
        if ("..".equals(actualResource.getName())) {
          actualSuperResource = actualResource;
          continue;
        }
        unexpectedResources.add(actualResource);
        continue;
      }

      DavResource expectedResource = mappedExpectedResources.get(uri);
      mappedExpectedResources.remove(uri);

      if (searchPath != null && searchPath.equals(actualResource.getHref())) {
        compare(actualResource, expectedResource, ".");
      } else {
        if (isResolvedToRootURI(actualResource.getHref(), authentication.getName())) {
          compare(actualResource, expectedResource, ".");
        } else {
          compare(actualResource, expectedResource, null);
        }
      }
    }
    assertThat(mappedExpectedResources).isEmpty();
    assertThat(unexpectedResources).isEmpty();

    if (isSuperUriExpected(searchPath)) {
      compare(actualSuperResource, expectedSuperResource, "..");
    }
  }

  private boolean isResolvedToRootURI(URI path, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (path.isAbsolute()) {
      return resolvedRootUri.equals(path);
    }
    return resolvedRootUri.equals(resolveAsDirectoryURI(path, username));
  }

  private void compare(OwncloudResource actualResource, DavResource expectedResource, String expectedName) {
    assertThat(expectedResource).isNotNull();
    assertThat(actualResource).isNotNull();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI uri = null;
    if (OwncloudUtils.isDirectory(actualResource)) {
      uri = resolveAsDirectoryURI(actualResource.getHref(), authentication.getName());
    } else {
      uri = resolveAsFileURI(actualResource.getHref(), authentication.getName());
    }
    uri = URI.create(uri.getPath());
    assertThat(uri).isEqualTo(expectedResource.getHref());
    if (StringUtils.isNotBlank(expectedName)) {
      assertThat(actualResource.getName()).isEqualTo(expectedName);
    } else {
      assertThat(actualResource.getName()).isEqualTo(expectedResource.getName());
    }
    assertThat(actualResource.getETag()).isEqualTo(expectedResource.getEtag());
    assertThat(actualResource.getMediaType().toString()).isEqualTo(expectedResource.getContentType());
    assertThat(actualResource.getLastModifiedAt()).isEqualTo(expectedResource.getModified());
  }

  private boolean isSuperUriExpected(URI searchPath) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return searchPath != null && !isResolvedToRootURI(searchPath, authentication.getName()) && properties.getResourceService().isAddRelativeDownPath();
  }

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_list_OK() throws Exception {
    URI searchPath = URI.create("/directory/directory/");
    List<DavResource> expectedResources = Lists.newArrayList(
        createDavResource("/directory/directory/", OwncloudUtils.getDirectoryMediaType(), null, "/", null),
        createDavResource("/directory/directory/resource1", MediaType.APPLICATION_PDF, Long.valueOf(1234), "resource1", Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenReturn(expectedResources);
    DavResource superResource = createDavResource("/directory/", OwncloudUtils.getDirectoryMediaType(), null, "..", null);
    Mockito
        .when(sardine.list(getResourcePath(URI.create("/directory/")), 0))
        .thenReturn(Lists.newArrayList(superResource));

    List<OwncloudResource> resources = resourceService.list(searchPath);
    compare(searchPath, resources, expectedResources, superResource);
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
}
