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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestOperations;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

/**
 * @author mufasa1976
 */
class OwncloudRestResourceServiceImpl implements OwncloudResourceService {

  private static final String PLACEHOLDER_USERNAME = "{username}";
  private static final String URI_SUFFIX = "/remote.php/dav/" + PLACEHOLDER_USERNAME;

  private final RestOperations restOperations;

  public OwncloudRestResourceServiceImpl(final RestTemplateBuilder builder, final OwncloudRestProperties properties) throws MalformedURLException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Validate.notNull(authentication);
    URL locationURL = OwncloudRestUtils.checkAndConvertLocation(properties.getLocation());
    String rootUri = getUri(locationURL, authentication.getName());
    restOperations = builder
        .basicAuthorization(authentication.getName(), (String) authentication.getCredentials())
        .rootUri(rootUri)
        .build();
  }

  protected String getUri(URL locationURL, String username) {
    if (StringUtils.contains(URI_SUFFIX, PLACEHOLDER_USERNAME)) {
      return getSuffixedLocation(locationURL, StringUtils.replace(URI_SUFFIX, PLACEHOLDER_USERNAME, username));
    }
    return getSuffixedLocation(locationURL, URI_SUFFIX);
  }

  private String getSuffixedLocation(URL locationURL, String suffix) {
    return StringUtils.stripEnd(locationURL.toString(), "/") + "/" + suffix;
  }

  @Override
  public List<OwncloudResource> listRoot() throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<OwncloudResource> list(Path relativeTo) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudModifiableResource createFile(Path file) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(Path directory) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(Path resource) throws OwncloudResourceException {
    // TODO Auto-generated method stub

  }

}
