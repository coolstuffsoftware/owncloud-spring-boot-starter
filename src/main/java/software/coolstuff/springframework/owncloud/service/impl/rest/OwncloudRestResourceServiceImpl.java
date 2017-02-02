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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestOperations;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudSardineCacheException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties.ResourceServiceProperties.CacheProperties;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudRestResourceServiceImpl implements OwncloudResourceService {

  private static final String URI_SUFFIX = "/remote.php/dav/files/{username}/";

  private final RestOperations restOperations;
  private final String rootUri;
  private final LoadingCache<String, Sardine> sardineCache;

  public OwncloudRestResourceServiceImpl(final RestTemplateBuilder builder, final OwncloudRestProperties properties) throws MalformedURLException {
    URL locationURL = OwncloudRestUtils.checkAndConvertLocation(properties.getLocation());
    rootUri = appendOptionalSuffix(locationURL, URI_SUFFIX);
    restOperations = builder
        .messageConverters(new ByteArrayHttpMessageConverter())
        .rootUri(rootUri)
        .build();
    sardineCache = buildCache(properties);
  }

  protected String appendOptionalSuffix(URL url, String suffix) {
    if (StringUtils.isBlank(suffix)) {
      return url.toString();
    }
    return StringUtils.stripEnd(url.toString(), "/") + "/" + StringUtils.stripStart(suffix, "/");
  }

  protected LoadingCache<String, Sardine> buildCache(final OwncloudProperties properties) {
    CacheProperties cacheProperties = properties.getResourceService().getCache();
    CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
    if (cacheProperties.getConcurrencyLevel() != null) {
      builder.concurrencyLevel(cacheProperties.getConcurrencyLevel());
    }
    if (cacheProperties.getExpireAfterAccess() != null && cacheProperties.getExpireAfterAccessTimeUnit() != null) {
      builder.expireAfterAccess(cacheProperties.getExpireAfterAccess(), cacheProperties.getExpireAfterAccessTimeUnit());
    }
    if (cacheProperties.getExpireAfterWrite() != null && cacheProperties.getExpireAfterWriteTimeUnit() != null) {
      builder.expireAfterWrite(cacheProperties.getExpireAfterWrite(), cacheProperties.getExpireAfterWriteTimeUnit());
    }
    if (cacheProperties.getInitialCapacity() != null) {
      builder.initialCapacity(cacheProperties.getInitialCapacity());
    }
    if (cacheProperties.getMaximumSize() != null) {
      builder.maximumSize(cacheProperties.getMaximumSize());
    }
    if (cacheProperties.getMaximumWeight() != null) {
      builder.maximumWeight(cacheProperties.getMaximumWeight());
    }
    if (cacheProperties.getRefreshAfterWrite() != null && cacheProperties.getRefreshAfterWriteTimeUnit() != null) {
      builder.refreshAfterWrite(cacheProperties.getRefreshAfterWrite(), cacheProperties.getRefreshAfterWriteTimeUnit());
    }
    return builder.build(new SardineCacheLoader(properties));
  }

  @Override
  public List<OwncloudResource> listRoot() throws OwncloudResourceException {
    return list(null);
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) throws OwncloudResourceException {
    URI resolvedRootUri = getResolvedRootUri();
    String path = resolvedRootUri.toString();
    if (relativeTo != null) {
      path = resolvedRootUri.resolve(relativeTo).toString();
    }
    List<OwncloudResource> resources = new ArrayList<>();
    try {
      for (DavResource davResource : getSardine().list(path)) {
        resources.add(createOwncloudResource(davResource, resolvedRootUri));
      }
    } catch (SardineException e) {
      throwMappedSardineException(URI.create(path), e);
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 9123944573421981304L;
      };
    }
    return resources;
  }

  private void throwMappedSardineException(URI uri, SardineException sardineException) throws OwncloudResourceException {
    int statusCode = Optional.fromNullable(sardineException)
        .transform(exception -> exception.getStatusCode())
        .or(HttpStatus.SC_OK);
    switch (statusCode) {
      case HttpStatus.SC_OK:
        return;
      case HttpStatus.SC_NOT_FOUND:
        throw new OwncloudResourceNotFoundException(uri);
      default:
        log.error("Unmapped HTTP-Status {}. Reason-Phrase: {}", statusCode, sardineException.getResponsePhrase());
        throw new OwncloudResourceException("Unmapped returned HTTP-Status " + statusCode) {
          private static final long serialVersionUID = 1738301120302156213L;
        };
    }
  }

  protected Sardine getSardine() throws OwncloudSardineCacheException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      return sardineCache.get(authentication.getName());
    } catch (Exception e) {
      throw new OwncloudSardineCacheException(e);
    }
  }

  private URI getResolvedRootUri() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return URI.create(StringUtils.replace(rootUri, "{username}", authentication.getName()));
  }

  private OwncloudResource createOwncloudResource(DavResource davResource, URI rootUri) {
    MediaType mediaType = MediaType.valueOf(davResource.getContentType());
    URI href = rootUri.resolve(davResource.getHref());
    href = rootUri.relativize(href);
    href = URI.create("/").resolve(href);
    OwncloudResource owncloudResource = OwncloudRestResourceImpl.builder()
        .href(href)
        .name(davResource.getName())
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
  public OwncloudResource find(URI path) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudFileResource createFile(URI file) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(URI directory) throws OwncloudResourceException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(OwncloudResource resource) throws OwncloudResourceException {
    // TODO Auto-generated method stub

  }

  @Override
  public InputStream getInputStream(OwncloudFileResource resource) throws OwncloudResourceException {
    return restOperations.execute(resource.getHref(), HttpMethod.GET, null, response -> response.getBody());
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) throws OwncloudResourceException {
    return new ContentOutputStream(restOperations, resource.getHref());
  }

  static class ContentOutputStream extends ByteArrayOutputStream {

    private final RestOperations restOperations;
    private final URI uri;

    public ContentOutputStream(final RestOperations restOperations, final URI uri) {
      super();
      this.restOperations = restOperations;
      this.uri = uri;
    }

    @Override
    public void close() throws IOException {
      super.close();
      restOperations.put(uri, this.toByteArray());
    }
  }

  @RequiredArgsConstructor
  static class SardineCacheLoader extends CacheLoader<String, Sardine> {

    private final OwncloudProperties properties;

    @Override
    public Sardine load(String username) throws Exception {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (OwncloudUtils.isAuthenticationClassNotSupported(authentication.getClass())) {
        throw new OwncloudInvalidAuthenticationObjectException(authentication);
      }
      if (!StringUtils.equals(username, authentication.getName())) {
        final String errorMessage = String.format("requested Username %s is not equal the Username of the SecurityContextHolder %s", username, authentication.getName());
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
      }
      return new SardineImpl(authentication.getName(), (String) authentication.getCredentials());
    }

  }
}
