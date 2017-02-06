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

import java.net.URI;
import java.util.List;

import com.github.sardine.DavResource;

import lombok.Builder;
import lombok.Data;

/**
 * @author mufasa1976
 */
public interface OwncloudRestResourceFactory {

  String getRestTemplateRootUri();

  URI resolveAsDirectoryURI(URI relativeTo, String username);

  URI resolveAsFileURI(URI relativeTo, String username);

  boolean isNotResolvedToRootURI(URI path, String username);

  boolean isResolvedToRootURI(URI path, String username);

  OwncloudNameModifyingRestResource createResourceFrom(DavResource davResource, OwncloudResourceConversionProperties conversionProperties);

  List<OwncloudNameModifyingRestResource> createResourcesFrom(List<DavResource> davResources, OwncloudResourceConversionProperties conversionProperties);

  @Data
  @Builder
  public class OwncloudResourceConversionProperties {
    private final String username;
    private URI searchPath;
    private String renamedSearchPath;
  }

}
