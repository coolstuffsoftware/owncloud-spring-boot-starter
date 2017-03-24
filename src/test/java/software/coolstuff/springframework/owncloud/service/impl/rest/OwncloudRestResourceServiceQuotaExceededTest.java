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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.Sardine;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceQuotaExceededTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

@ActiveProfiles("REST-RESOURCE-SERVICE")
public class OwncloudRestResourceServiceQuotaExceededTest extends AbstractOwncloudResourceServiceQuotaExceededTest implements OwncloudRestServiceTest {

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

}
