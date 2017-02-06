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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;

/**
 * @author mufasa1976
 */
class OwncloudRestResourceFactoryImpl implements OwncloudRestResourceFactory {

  private static final String URI_SUFFIX = "/remote.php/dav/files/{username}/";

  private final OwncloudRestProperties properties;
  private final String rootUri;

  public OwncloudRestResourceFactoryImpl(final OwncloudRestProperties properties) throws MalformedURLException {
    this.properties = properties;
    URL locationURL = OwncloudRestUtils.checkAndConvertLocation(properties.getLocation());
    rootUri = appendOptionalSuffix(locationURL, URI_SUFFIX);
  }

  protected String appendOptionalSuffix(URL url, String suffix) {
    if (StringUtils.isBlank(suffix)) {
      return url.toString();
    }
    return StringUtils.stripEnd(url.toString(), "/") + "/" + StringUtils.stripStart(suffix, "/");
  }

  @Override
  public String getRestTemplateRootUri() {
    return rootUri;
  }

  @Override
  public URI resolveAsDirectoryURI(URI relativeTo, String username) {
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
    return URI.create(StringUtils.replace(rootUri, "{username}", username));
  }

  @Override
  public URI resolveAsFileURI(URI relativeTo, String username) {
    Validate.notNull(relativeTo);
    return URI.create(
        UriComponentsBuilder.fromUri(getResolvedRootUri(username))
            .path(relativeTo.getPath())
            .toUriString());
  }

  @Override
  public boolean isNotResolvedToRootURI(URI path, String username) {
    return !isResolvedToRootURI(path, username);
  }

  @Override
  public boolean isResolvedToRootURI(URI path, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (path.isAbsolute()) {
      return resolvedRootUri.equals(path);
    }
    return resolvedRootUri.equals(resolveAsDirectoryURI(path, username));
  }

  @Override
  public OwncloudNameModifyingRestResource createResourceFrom(DavResource davResource, OwncloudResourceConversionProperties conversionProperties) {
    URI rootUri = getResolvedRootUri(conversionProperties.getUsername());
    return createResourceFrom(davResource, rootUri, conversionProperties);
  }

  private OwncloudNameModifyingRestResource createResourceFrom(DavResource davResource, URI rootUri, OwncloudResourceConversionProperties conversionProperties) {
    MediaType mediaType = MediaType.valueOf(davResource.getContentType());
    URI href = rootUri.resolve(davResource.getHref());
    String name = davResource.getName();
    if (davResource.isDirectory() && href.equals(rootUri)) {
      name = "/";
    }
    href = rootUri.relativize(href);
    href = URI.create("/").resolve(href).normalize(); // prepend / to the href
    OwncloudNameModifyingRestResource owncloudResource = OwncloudRestResourceImpl.builder()
        .href(href)
        .name(name)
        .lastModifiedAt(davResource.getModified())
        .mediaType(mediaType)
        .eTag(StringUtils.strip(davResource.getEtag(), "\""))
        .build();
    if (davResource.isDirectory()) {
      return owncloudResource;
    }
    return OwncloudRestFileResourceImpl.fileBuilder()
        .owncloudResource(owncloudResource)
        .contentLength(davResource.getContentLength())
        .build();
  }

  @Override
  public List<OwncloudNameModifyingRestResource> createResourcesFrom(List<DavResource> davResources, OwncloudResourceConversionProperties conversionProperties) {
    URI rootUri = getResolvedRootUri(conversionProperties.getUsername());
    List<OwncloudNameModifyingRestResource> resources = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(davResources)) {
      for (DavResource davResource : davResources) {
        OwncloudNameModifyingRestResource resource = createResourceFrom(davResource, rootUri, conversionProperties);
        if (isSearchPath(resource, rootUri, conversionProperties.getSearchPath()) && StringUtils.isNotBlank(conversionProperties.getRenamedSearchPath())) {
          resource.setName(conversionProperties.getRenamedSearchPath());
        }
        resources.add(resource);
      }
    }
    return resources;
  }

  private boolean isSearchPath(OwncloudNameModifyingRestResource resource, URI rootUri, URI searchPath) {
    URI resourcePath = URI.create(
        UriComponentsBuilder.fromUri(rootUri)
            .path(resource.getHref().getPath())
            .toUriString())
        .normalize();
    return searchPath.equals(resourcePath);
  }
}
