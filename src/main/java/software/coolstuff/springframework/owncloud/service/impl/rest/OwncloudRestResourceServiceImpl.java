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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudSardineCacheException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties.ResourceServiceProperties.CacheProperties;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestResourceFactory.OwncloudResourceConversionProperties;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudRestResourceServiceImpl implements OwncloudResourceService {

  private final RestOperations restOperations;
  private final OwncloudRestProperties properties;
  private final OwncloudRestResourceFactory resourceFactory;

  private LoadingCache<String, Sardine> sardineCache;

  @Autowired
  private SardineCacheLoader sardineCacheLoader;

  public OwncloudRestResourceServiceImpl(
      final RestTemplateBuilder builder,
      final OwncloudRestResourceFactory resourceFactory,
      final OwncloudRestProperties properties) throws MalformedURLException {
    this.properties = properties;
    this.resourceFactory = resourceFactory;
    restOperations = builder
        .messageConverters(new ByteArrayHttpMessageConverter())
        .rootUri(resourceFactory.getRestTemplateRootUri())
        .build();
  }

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    sardineCache = buildCache();
  }

  protected LoadingCache<String, Sardine> buildCache() {
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
    return builder.build(sardineCacheLoader);
  }

  @Override
  public List<OwncloudResource> list(URI relativeTo) throws OwncloudResourceException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI searchPath = resourceFactory.resolveAsDirectoryURI(relativeTo, authentication.getName());
    OwncloudResourceConversionProperties conversionProperties = OwncloudRestResourceFactory.OwncloudResourceConversionProperties.builder()
        .searchPath(searchPath)
        .renamedSearchPath(".")
        .username(authentication.getName())
        .build();
    List<OwncloudResource> resources = new ArrayList<>();
    try {
      Sardine sardine = getSardine();
      List<DavResource> davResources = sardine.list(searchPath.toString());
      resources.addAll(resourceFactory.createResourcesFrom(davResources, conversionProperties));
      if (properties.getResourceService().isAddRelativeDownPath() && resourceFactory.isNotResolvedToRootURI(searchPath, authentication.getName())) {
        searchPath = URI.create(
            UriComponentsBuilder.fromUri(searchPath.normalize())
                .path("/../")
                .toUriString())
            .normalize();
        conversionProperties = OwncloudRestResourceFactory.OwncloudResourceConversionProperties.builder()
            .searchPath(searchPath)
            .renamedSearchPath("..")
            .username(authentication.getName())
            .build();
        resources.addAll(resourceFactory.createResourcesFrom(sardine.list(searchPath.toString(), 0), conversionProperties));
      }
    } catch (SardineException e) {
      throwMappedSardineException(URI.create(searchPath.toString()), e);
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 9123944573421981304L;
      };
    }
    return resources;
  }

  protected Sardine getSardine() throws OwncloudSardineCacheException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      return sardineCache.get(authentication.getName());
    } catch (Exception e) {
      throw new OwncloudSardineCacheException(e);
    }
  }

  private void throwMappedSardineException(URI uri, SardineException sardineException) throws OwncloudResourceException {
    int statusCode = Optional.ofNullable(sardineException)
        .map(exception -> exception.getStatusCode())
        .orElse(HttpStatus.SC_OK);
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

  @Override
  public OwncloudResource find(URI path) throws OwncloudResourceException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI searchPath = resourceFactory.resolveAsDirectoryURI(path, authentication.getName());
    OwncloudResourceConversionProperties conversionProperties = OwncloudRestResourceFactory.OwncloudResourceConversionProperties.builder()
        .searchPath(searchPath)
        .username(authentication.getName())
        .build();
    OwncloudResource resource = null;
    try {
      for (DavResource davResource : getSardine().list(searchPath.toString(), 0)) {
        resource = resourceFactory.createResourceFrom(davResource, conversionProperties);
      }
    } catch (SardineException e) {
      throwMappedSardineException(URI.create(path.toString()), e);
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 9123944573421981304L;
      };
    }
    return resource;
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
}
