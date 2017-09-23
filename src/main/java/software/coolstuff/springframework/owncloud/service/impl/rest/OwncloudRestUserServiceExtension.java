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

import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;

/**
 * Extensions of {@link OwncloudUserService} for the REST Service
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public interface OwncloudRestUserServiceExtension extends OwncloudUserService {

  /**
   * Get the Quota of the given User
   * @param username Username of the User
   * @return Quota of the User
   */
  OwncloudRestQuotaImpl getQuota(String username);

}
