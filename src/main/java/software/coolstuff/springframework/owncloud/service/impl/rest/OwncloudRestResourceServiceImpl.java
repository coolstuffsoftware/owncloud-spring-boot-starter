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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import lombok.Builder;
import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudSardineCacheException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties.ResourceServiceProperties.CacheProperties;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudRestResourceServiceImpl implements OwncloudResourceService {

  private static final String URI_SUFFIX = "/remote.php/dav/files/{username}/";

  private final RestOperations restOperations;
  private final OwncloudRestProperties properties;
  private final String rootUri;

  private LoadingCache<String, Sardine> sardineCache;

  @Autowired
  private SardineCacheLoader sardineCacheLoader;

  public OwncloudRestResourceServiceImpl(
      final RestTemplateBuilder builder,
      final OwncloudRestProperties properties) throws MalformedURLException {
    this.properties = properties;
    URL locationURL = OwncloudRestUtils.checkAndConvertLocation(properties.getLocation());
    rootUri = appendOptionalSuffix(locationURL, URI_SUFFIX);
    restOperations = builder
        .messageConverters(new ByteArrayHttpMessageConverter())
        .rootUri(rootUri)
        .build();
  }

  protected String appendOptionalSuffix(URL url, String suffix) {
    if (StringUtils.isBlank(suffix)) {
      return url.toString();
    }
    return StringUtils.stripEnd(url.toString(), "/") + "/" + StringUtils.stripStart(suffix, "/");
  }

  RestTemplate getRestTemplate() {
    return (RestTemplate) restOperations;
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
  public List<OwncloudResource> list(URI relativeTo) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI searchPath = resolveAsDirectoryURI(relativeTo, authentication.getName());
    List<OwncloudResource> owncloudResources = new ArrayList<>();
    try {
      Sardine sardine = getSardine();
      URI rootPath = getResolvedRootUri(authentication.getName());
      List<DavResource> davResources = sardine.list(searchPath.toString());
      val sarchPathConversionProperties = OwncloudResourceConversionProperties.builder()
          .rootPath(rootPath)
          .searchPath(searchPath)
          .renamedSearchPath(".")
          .build();
      owncloudResources.addAll(
          davResources.stream()
              .map(davResource -> createOwncloudResourceFrom(davResource, sarchPathConversionProperties))
              .map(modifyingResource -> renameOwncloudResource(modifyingResource, sarchPathConversionProperties))
              .collect(Collectors.toList()));
      if (isAddSuperResourceToCollection(searchPath, owncloudResources)) {
        searchPath = URI.create(
            UriComponentsBuilder.fromUri(searchPath.normalize())
                .path("/../")
                .toUriString())
            .normalize();
        val parentDirectoryConversionProperties = OwncloudResourceConversionProperties.builder()
            .rootPath(rootPath)
            .searchPath(searchPath)
            .renamedSearchPath("..")
            .build();
        davResources = sardine.list(searchPath.toString(), 0);
        owncloudResources.addAll(
            davResources.stream()
                .map(davResource -> createOwncloudResourceFrom(davResource, parentDirectoryConversionProperties))
                .map(modifyingResource -> renameOwncloudResource(modifyingResource, parentDirectoryConversionProperties))
                .collect(Collectors.toList()));
      }
    } catch (SardineException e) {
      throwMappedSardineException(URI.create(searchPath.toString()), authentication, e);
    } catch (IOException e) {
      throw new OwncloudRestResourceException(e);
    }
    return owncloudResources;
  }

  public URI resolveAsDirectoryURI(URI relativeTo, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return resolvedRootUri;
    }
    return URI.create(
        UriComponentsBuilder.fromUri(resolvedRootUri)
            .path(relativeTo.getPath())
            .path("/")
            .toUriString())
        .normalize();
  }

  public URI getResolvedRootUri(String username) {
    return URI.create(StringUtils.replace(rootUri, "{username}", username));
  }

  protected Sardine getSardine() throws OwncloudSardineCacheException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      return sardineCache.get(authentication.getName());
    } catch (Exception e) {
      throw new OwncloudSardineCacheException(e);
    }
  }

  @Data
  @Builder
  static class OwncloudResourceConversionProperties {
    private final URI rootPath;
    private URI searchPath;
    private String renamedSearchPath;
  }

  public OwncloudModifyingRestResource createOwncloudResourceFrom(DavResource davResource, OwncloudResourceConversionProperties conversionProperties) {
    MediaType mediaType = MediaType.valueOf(davResource.getContentType());
    URI rootPath = conversionProperties.getRootPath();
    URI href = rootPath.resolve(davResource.getHref());
    String name = davResource.getName();
    if (davResource.isDirectory() && href.equals(rootPath)) {
      name = "/";
    }
    href = rootPath.relativize(href);
    href = URI.create("/").resolve(href).normalize(); // prepend "/" to the href
    OwncloudModifyingRestResource owncloudResource = OwncloudRestResourceImpl.builder()
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

  public OwncloudModifyingRestResource renameOwncloudResource(OwncloudModifyingRestResource resource, OwncloudResourceConversionProperties conversionProperties) {
    if (StringUtils.isBlank(conversionProperties.getRenamedSearchPath())) {
      return resource;
    }
    URI resourcePath = URI.create(
        UriComponentsBuilder.fromUri(conversionProperties.getRootPath())
            .path(resource.getHref().getPath())
            .toUriString())
        .normalize();
    if (conversionProperties.getSearchPath().equals(resourcePath)) {
      resource.setName(conversionProperties.getRenamedSearchPath());
    }
    return resource;
  }

  private boolean isNotResolvedToRootURI(URI path, String username) {
    return !isResolvedToRootURI(path, username);
  }

  private boolean isResolvedToRootURI(URI path, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (path.isAbsolute()) {
      return resolvedRootUri.equals(path);
    }
    return resolvedRootUri.equals(resolveAsDirectoryURI(path, username));
  }

  private boolean isAddSuperResourceToCollection(URI searchPath, List<OwncloudResource> owncloudResources) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return properties.getResourceService().isAddRelativeDownPath()
        && isNotResolvedToRootURI(searchPath, authentication.getName())
        && containsNotOnlyOneFileResource(owncloudResources);
  }

  private boolean containsNotOnlyOneFileResource(List<OwncloudResource> owncloudResources) {
    return !containsOnlyOneFileResource(owncloudResources);
  }

  private boolean containsOnlyOneFileResource(List<OwncloudResource> owncloudResources) {
    return owncloudResources.size() == 1 && !OwncloudUtils.isDirectory(owncloudResources.get(0));
  }

  private void throwMappedSardineException(URI uri, Authentication authentication, SardineException sardineException) throws OwncloudResourceException {
    int statusCode = Optional.ofNullable(sardineException)
        .map(exception -> exception.getStatusCode())
        .orElse(HttpStatus.SC_OK);
    switch (statusCode) {
      case HttpStatus.SC_OK:
        return;
      case HttpStatus.SC_NOT_FOUND:
        throw new OwncloudResourceNotFoundException(uri, authentication.getName());
      default:
        log.error("Unmapped HTTP-Status {}. Reason-Phrase: {}", statusCode, sardineException.getResponsePhrase());
        throw new OwncloudRestResourceException("Unmapped returned HTTP-Status " + statusCode, sardineException);
    }
  }

  @Override
  public OwncloudResource find(URI path) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    URI searchPath = resolveAsDirectoryURI(path, authentication.getName());
    val conversionProperties = OwncloudResourceConversionProperties.builder()
        .rootPath(getResolvedRootUri(authentication.getName()))
        .searchPath(searchPath)
        .build();
    OwncloudResource owncloudResource = null;
    try {
      Sardine sardine = getSardine();
      List<DavResource> davResources = sardine.list(searchPath.toString(), 0);
      owncloudResource = davResources.stream()
          .findFirst()
          .map(davResource -> createOwncloudResourceFrom(davResource, conversionProperties))
          .orElseThrow(() -> new OwncloudResourceNotFoundException(path, authentication.getName()));
    } catch (SardineException e) {
      throwMappedSardineException(URI.create(path.toString()), authentication, e);
    } catch (IOException e) {
      throw new OwncloudRestResourceException(e);
    }
    return owncloudResource;
  }

  @Override
  public OwncloudFileResource createFile(URI file) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OwncloudResource createDirectory(URI directory) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(OwncloudResource resource) {
    // TODO Auto-generated method stub

  }

  @Override
  public InputStream getInputStream(OwncloudFileResource resource) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PipedInputStreamSynchronizer pipedInputStreamSynchronizer = PipedInputStreamSynchronizer.build()
        .authentication(authentication)
        .owncloudFileResource(resource)
        .owncloudRestProperties(properties)
        .restOperations(restOperations)
        .uriResolver(this::resolveAsFileURI)
        .build();
    return pipedInputStreamSynchronizer.getInputStream();
  }

  private URI resolveAsFileURI(URI relativeTo, String username) {
    URI resolvedRootUri = getResolvedRootUri(username);
    if (relativeTo == null || StringUtils.isBlank(relativeTo.getPath())) {
      return resolvedRootUri;
    }
    return URI.create(
        UriComponentsBuilder.fromUri(resolvedRootUri)
            .path(relativeTo.getPath())
            .toUriString())
        .normalize();
  }

  @Override
  public OutputStream getOutputStream(OwncloudFileResource resource) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    PipedOutputStreamSynchronizer pipedOutputStreamSynchronizer = PipedOutputStreamSynchronizer.builder()
        .authentication(authentication)
        .owncloudFileResource(resource)
        .owncloudRestProperties(properties)
        .restOperations(restOperations)
        .uriResolver(this::resolveAsFileURI)
        .build();
    return pipedOutputStreamSynchronizer.getOutputStream();
  }
}
