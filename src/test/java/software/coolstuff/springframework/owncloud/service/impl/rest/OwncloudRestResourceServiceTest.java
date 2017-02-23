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

import lombok.val;
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
  private OwncloudRestResourceServiceImpl resourceService;

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
    List<DavResource> expectedResources = Lists.newArrayList(
        createDavResource("/", OwncloudUtils.getDirectoryMediaType(), null, "/", null),
        createDavResource("/resource1", MediaType.APPLICATION_PDF, Long.valueOf(1234), "resource1", Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath()))
        .thenReturn(expectedResources);
    return createFrom(null, expectedResources);
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

  private URI resolveAsFileURI(URI relativeTo, String username) {
    return URI.create(
        UriComponentsBuilder.fromUri(resourceService.getResolvedRootUri(username))
            .path(relativeTo.getPath())
            .toUriString());
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
    URI resolvedRootUri = resourceService.getResolvedRootUri(username);
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return resolvedRootUri;
    }
    return URI.create(
        UriComponentsBuilder.fromUri(resolvedRootUri)
            .path(relativeTo.getPath())
            .path("/")
            .toUriString());
  }

  private List<OwncloudResource> createFrom(URI relativeTo, List<DavResource> davResources) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI rootUri = resourceService.getResolvedRootUri(authentication.getName());
    URI searchPath = resolveSearchPath(relativeTo, rootUri);
    val conversionProperties = OwncloudRestResourceServiceImpl.OwncloudResourceConversionProperties.builder()
        .rootPath(rootUri)
        .searchPath(searchPath)
        .renamedSearchPath(".")
        .build();
    return davResources.stream()
        .map(davResource -> resourceService.createOwncloudResourceFrom(davResource, conversionProperties))
        .map(owncloudResource -> resourceService.renameOwncloudResource(owncloudResource, conversionProperties))
        .collect(Collectors.toList());
  }

  private URI resolveSearchPath(URI relativeTo, URI rootUri) {
    URI searchPath = URI.create(
        UriComponentsBuilder.fromUri(rootUri)
            .path(Optional.ofNullable(relativeTo).map(uri -> uri.getPath()).orElse("/"))
            .toUriString());
    return searchPath;
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
    List<DavResource> davResources = Lists.newArrayList(
        createDavResource("/directory/directory/", OwncloudUtils.getDirectoryMediaType(), null, "/", null),
        createDavResource("/directory/directory/resource1", MediaType.APPLICATION_PDF, Long.valueOf(1234), "resource1", Locale.GERMAN));
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenReturn(davResources);
    DavResource davSuperResource = createDavResource("/directory/", OwncloudUtils.getDirectoryMediaType(), null, "..", null);
    Mockito
        .when(sardine.list(getResourcePath(URI.create("/directory/")), 0))
        .thenReturn(Lists.newArrayList(davSuperResource));

    List<OwncloudResource> expectedResources = createFrom(searchPath, davResources);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI rootUri = resourceService.getResolvedRootUri(authentication.getName());
    URI superSearchPath = resolveSearchPath(
        URI.create(
            UriComponentsBuilder.fromUri(searchPath)
                .path("/../")
                .toUriString())
            .normalize(),
        rootUri);
    val conversionProperties = OwncloudRestResourceServiceImpl.OwncloudResourceConversionProperties.builder()
        .rootPath(rootUri)
        .searchPath(superSearchPath)
        .renamedSearchPath("..")
        .build();
    OwncloudModifyingRestResource owncloudSuperResource = resourceService.createOwncloudResourceFrom(davSuperResource, conversionProperties);
    owncloudSuperResource = resourceService.renameOwncloudResource(owncloudSuperResource, conversionProperties);
    expectedResources.add(owncloudSuperResource);
    return expectedResources;
  }
}
