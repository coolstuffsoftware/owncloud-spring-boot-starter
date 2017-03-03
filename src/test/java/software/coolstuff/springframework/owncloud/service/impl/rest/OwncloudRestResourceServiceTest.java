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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
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
  protected void prepare_list_NOK_FileNoutFound(URI searchPath) throws Exception {
    Mockito
        .when(sardine.list(getResourcePath(searchPath)))
        .thenThrow(new SardineException("Resource not found", HttpStatus.SC_NOT_FOUND, "Resource not found"));
  }

}
