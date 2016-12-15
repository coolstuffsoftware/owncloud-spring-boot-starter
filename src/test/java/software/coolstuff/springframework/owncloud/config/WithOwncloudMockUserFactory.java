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
package software.coolstuff.springframework.owncloud.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class WithOwncloudMockUserFactory implements WithSecurityContextFactory<WithOwncloudMockUser> {

  @Override
  public SecurityContext createSecurityContext(WithOwncloudMockUser withMockOwncloudUser) {
    String username = StringUtils.isNotBlank(withMockOwncloudUser.username()) ? withMockOwncloudUser.username()
        : withMockOwncloudUser.value();
    Validate.notBlank(username);

    OwncloudUserDetails owncloudUserDetails = OwncloudUserDetails.builder().username(username)
        .password(withMockOwncloudUser.password()).build();

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new OwncloudAuthentication(owncloudUserDetails));
    return context;
  }

}
