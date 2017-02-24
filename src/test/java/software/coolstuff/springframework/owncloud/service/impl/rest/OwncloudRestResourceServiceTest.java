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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
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

import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
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
    List<OwncloudResource> expected = prepare_listRoot_OK();
    List<OwncloudResource> resources = resourceService.listRoot();
    assertThat(resources).containsAll(expected);
  }

  protected List<OwncloudResource> prepare_listRoot_OK() throws Exception {
    List<OwncloudResource> expectedOwncloudResources = Lists.newArrayList(
        OwncloudRestResourceImpl.builder()
            .eTag(UUID.randomUUID().toString())
            .href(URI.create("/"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name(".")
            .build(),
        OwncloudRestFileResourceImpl.fileBuilder()
            .owncloudResource(OwncloudRestFileResourceImpl.builder()
                .eTag(UUID.randomUUID().toString())
                .href(URI.create("/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_PDF)
                .name("resource1")
                .build())
            .contentLength(Long.valueOf(1234))
            .build());
    List<DavResource> expectedDavResources = expectedOwncloudResources.stream()
        .map(owncloudResource -> createDavResourceFrom(owncloudResource, Locale.GERMAN, "."))
        .collect(Collectors.toList());
    Mockito
        .when(sardine.list(getResourcePath()))
        .thenReturn(expectedDavResources);
    return expectedOwncloudResources;
  }

  private DavResource createDavResourceFrom(OwncloudResource owncloudResource, Locale locale, String renameName) {
    URI prefixedHref = resolveAsFileURI(owncloudResource.getHref());
    String contentLanguage = Optional.ofNullable(locale)
        .map(loc -> loc.getLanguage())
        .orElse(null);
    String name = owncloudResource.getName();
    if (StringUtils.equals(name, renameName)) {
      String[] tokenizedPath = StringUtils.split(owncloudResource.getHref().getPath(), "/");
      if (ArrayUtils.isNotEmpty(tokenizedPath)) {
        name = tokenizedPath[tokenizedPath.length - 1];
      } else {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        name = authentication.getName();
      }
    }
    try {
      return new OwncloudDavResource(
          prefixedHref.getPath(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getMediaType().toString(),
          (owncloudResource instanceof OwncloudFileResource ? ((OwncloudFileResource) owncloudResource).getContentLength() : null),
          owncloudResource.getETag(),
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

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_list_OK() throws Exception {
    URI searchPath = URI.create("/directory/directory/");
    List<OwncloudResource> expected = prepare_list_OK(searchPath);
    List<OwncloudResource> resources = resourceService.list(searchPath);
    assertThat(resources).containsAll(expected);
  }

  protected List<OwncloudResource> prepare_list_OK(URI searchPath) throws Exception {
    List<OwncloudResource> expectedOwncloudResources = Lists.newArrayList(
        OwncloudRestResourceImpl.builder()
            .eTag(UUID.randomUUID().toString())
            .href(appendPath(searchPath, "/"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name(".")
            .build(),
        OwncloudRestFileResourceImpl.fileBuilder()
            .owncloudResource(OwncloudRestResourceImpl.builder()
                .eTag(UUID.randomUUID().toString())
                .href(appendPath(searchPath, "/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_PDF)
                .name("resource1")
                .build())
            .contentLength(Long.valueOf(1234))
            .build());
    List<DavResource> expectedDavResources = expectedOwncloudResources.stream()
        .map(owncloudResource -> createDavResourceFrom(owncloudResource, Locale.GERMAN, "."))
        .collect(Collectors.toList());
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenReturn(expectedDavResources);

    if (isNotRoot(searchPath) && properties.getResourceService().isAddRelativeDownPath()) {
      URI superSearchPath = URI.create(
          UriComponentsBuilder.fromUri(searchPath)
              .path("/../")
              .toUriString())
          .normalize();
      OwncloudResource owncloudSuperResource = OwncloudRestResourceImpl.builder()
          .eTag(UUID.randomUUID().toString())
          .href(superSearchPath)
          .lastModifiedAt(new Date())
          .mediaType(OwncloudUtils.getDirectoryMediaType())
          .name("..")
          .build();
      expectedOwncloudResources.add(owncloudSuperResource);
      DavResource superDavResource = createDavResourceFrom(owncloudSuperResource, Locale.GERMAN, "..");
      Mockito
          .when(sardine.list(getResourcePath(URI.create("/directory/")), 0))
          .thenReturn(Lists.newArrayList(superDavResource));
    }

    return expectedOwncloudResources;
  }

  private URI appendPath(URI baseUri, String appendPath) {
    if (baseUri == null) {
      return Optional.ofNullable(appendPath)
          .map(path -> URI.create(path))
          .orElse(baseUri);
    }
    return URI.create(
        UriComponentsBuilder.fromUri(baseUri)
            .path(appendPath)
            .toUriString());
  }

  private boolean isNotRoot(URI searchPath) {
    return !"/".equals(searchPath.getPath());
  }
}
