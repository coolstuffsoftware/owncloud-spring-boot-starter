/*
   Copyright (C) 2016 by the original Authors.

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
package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

/**
 * Map the {@link GrantedAuthoritiesMapper} or the {@link OwncloudGrantedAuthoritiesMapper} to the given UserDetails
 *
 * @author mufasa1976
 */
public interface OwncloudUserDetailsMappingService {

  /**
   * Map the {@link GrantedAuthoritiesMapper} or the {@link OwncloudGrantedAuthoritiesMapper} to the given UserDetails
   *
   * @param userDetails UserDetails to be modified
   */
  void mapGrantedAuthorities(OwncloudUserDetails userDetails);

}
