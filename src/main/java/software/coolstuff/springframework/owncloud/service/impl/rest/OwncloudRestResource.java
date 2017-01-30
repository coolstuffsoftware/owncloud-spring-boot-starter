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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Optional;

import com.github.sardine.DavResource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudUnmodifiableResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;

/**
 * @author mufasa1976
 */
@RequiredArgsConstructor
class OwncloudRestResource implements OwncloudModifiableResource {

  private final URI href;
  private final Date creationAt;
  private Date lastModifiedAt;
  @Getter
  private final boolean directory;
  private DavResource davResource;

  public OwncloudRestResource(final DavResource davResource) {
    this.href = davResource.getHref();
    this.lastModifiedAt = this.creationAt = new Date();
    this.directory = davResource.isDirectory();
    this.davResource = davResource;
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
  public Date getCreationAt() {
    return Optional.ofNullable(davResource)
        .map(davResource -> davResource.getCreation())
        .orElse(creationAt);
  }

  @Override
  public Date getLastModifiedAt() {
    return Optional.ofNullable(davResource)
        .map(davResourcde -> davResource.getModified())
        .orElse(lastModifiedAt);
  }

  @Override
  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getETag() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long getContentLength() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public InputStream getContent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutputStream getOutputStream() {
    // TODO Auto-generated method stub
    return null;
  }

}
