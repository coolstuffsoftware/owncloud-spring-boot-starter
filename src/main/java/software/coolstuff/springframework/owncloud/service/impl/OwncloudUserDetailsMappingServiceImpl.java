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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@Slf4j
public class OwncloudUserDetailsMappingServiceImpl implements OwncloudUserDetailsMappingService {

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Override
  public void mapGrantedAuthorities(OwncloudUserDetails userDetails) {
    if (owncloudGrantedAuthoritiesMapper != null) {
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), OwncloudGrantedAuthoritiesMapper.class, owncloudGrantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), GrantedAuthoritiesMapper.class, grantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
    }
  }
}
