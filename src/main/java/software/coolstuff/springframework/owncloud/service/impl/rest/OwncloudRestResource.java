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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestOperations;

import com.github.sardine.DavResource;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudUnmodifiableResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;

/**
 * @author mufasa1976
 */
@Data
@Setter(AccessLevel.PACKAGE)
class OwncloudRestResource implements OwncloudModifiableResource {

  private static final long DEFAULT_LENGTH = -1;

  @Getter(AccessLevel.NONE)
  private final RestOperations restOperations;
  @Getter(AccessLevel.NONE)
  private final URI href;
  private final boolean directory;
  private final Date creationAt;
  private Date lastModifiedAt;
  private MediaType mediaType;
  private String eTag;
  private long contentLength = DEFAULT_LENGTH;
  @Getter(AccessLevel.NONE)
  private byte[] content;

  @Builder
  public OwncloudRestResource(
      final RestOperations restOperations,
      final URI href,
      boolean directory,
      final MediaType mediaType,
      final String eTag) {
    this.restOperations = restOperations;
    this.href = href;
    this.lastModifiedAt = this.creationAt = new Date();
    this.directory = directory;
    this.mediaType = mediaType;
    this.eTag = eTag;
  }

  public OwncloudRestResource(final RestOperations restOperations, final DavResource davResource) {
    this.restOperations = restOperations;
    this.href = davResource.getHref();
    this.creationAt = davResource.getCreation();
    this.lastModifiedAt = davResource.getModified();
    this.directory = davResource.isDirectory();
    this.contentLength = davResource.getContentLength();
  }

  @Override
  public boolean isModifiable() {
    return !isDirectory();
  }

  @Override
  public OwncloudModifiableResource getModifiableResource() throws OwncloudUnmodifiableResourceException {
    if (!isModifiable()) {
      throw new OwncloudUnmodifiableResourceException();
    }
    return this;
  }

  @Override
  public Long getContentLength() {
    if (content == null) {
      return contentLength;
    }
    return Long.valueOf(content.length);
  }

  @Override
  public InputStream getContent() {
    if (content != null) {
      return new ByteArrayInputStream(content);
    }
    return restOperations.execute(href, HttpMethod.GET, request -> {
      val headers = request.getHeaders();
      headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }, response -> response.getBody());
  }

  @Override
  public OutputStream getOutputStream() {
    // TODO Auto-generated method stub
    return null;
  }

}
