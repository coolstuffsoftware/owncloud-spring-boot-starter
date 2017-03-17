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

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.impl.rest.Ocs.User.Data.Quota;

@Slf4j
class OwncloudRestQuotaServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudRestQuotaService {

  protected OwncloudRestQuotaServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public OwncloudRestQuota getQuota(String username) {
    log.debug("Get Information about User {} from Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    Quota quota = user.getData().getQuota();
    return OwncloudRestQuota.builder()
        .free(quota.getFree())
        .used(quota.getUsed())
        .total(quota.getTotal())
        .relative(quota.getRelative())
        .build();
  }

}
