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

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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

  private static final String URI_PREFIX = "/remote.php/dav/files/{username}";

  @MockBean
  private SardineCacheLoader sardineCacheLoader;

  @Mock
  private Sardine sardine;

  @Autowired
  private OwncloudResourceService resourceService;

  @Before
  public void setUp() throws Exception {
    Mockito.when(sardineCacheLoader.load(Mockito.anyString()))
        .thenReturn(sardine);
  }

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_listRoot_OK() throws Exception {
    List<DavResource> expectedResources = Lists.newArrayList(
        createDavResource("/", OwncloudUtils.getDirectoryMediaType(), null, "/", null),
        createDavResource("/resource1", MediaType.APPLICATION_PDF, Long.valueOf(1234), "resource1", Locale.GERMAN));
    Mockito.when(sardine.list(Mockito.anyString()))
        .thenReturn(expectedResources);
    Mockito.when(sardine.list(Mockito.anyString(), Mockito.eq(0)))
        .thenThrow(new RuntimeException("this should not be called"));

    List<OwncloudResource> resources = resourceService.listRoot();
    assertThat(resources).hasSameSizeAs(expectedResources);
  }

  private DavResource createDavResource(
      String href,
      MediaType mediaType,
      Long contentLength,
      String displayName,
      Locale locale) throws URISyntaxException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String resolvedUriPrefix = StringUtils.replace(URI_PREFIX, "{username}", authentication.getName());
    String prefixedHref = UriComponentsBuilder.newInstance()
        .path(resolvedUriPrefix)
        .path(href)
        .toUriString();
    String contentLanguage = Optional.ofNullable(locale)
        .map(loc -> loc.getLanguage())
        .orElse(null);
    return new OwncloudDavResource(
        prefixedHref,
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
}
