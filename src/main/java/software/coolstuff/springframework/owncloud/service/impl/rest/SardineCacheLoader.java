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

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;
import com.google.common.cache.CacheLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class SardineCacheLoader extends CacheLoader<String, Sardine> {

  @Override
  public Sardine load(String username) throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!StringUtils.equals(username, authentication.getName())) {
      final String errorMessage = String.format("requested Username %s does not equal to the Username of the SecurityContextHolder %s", username, authentication.getName());
      log.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }
    return new SardineImpl(authentication.getName(), (String) authentication.getCredentials());
  }

}
