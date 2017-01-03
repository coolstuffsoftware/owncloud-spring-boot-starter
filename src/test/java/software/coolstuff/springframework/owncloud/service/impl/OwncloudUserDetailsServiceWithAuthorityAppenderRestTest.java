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

import static org.springframework.http.HttpMethod.GET;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("AUTHORITY-APPENDER-URL")
public class OwncloudUserDetailsServiceWithAuthorityAppenderRestTest extends AbstractOwncloudUserDetailsServiceWithAuthorityAppenderTest implements OwncloudServiceRestTest {

  @Autowired
  private UserDetailsService userDetailsService;

  @Override
  public OwncloudRestService owncloudService() {
    return (OwncloudRestService) userDetailsService;
  }

  @Override
  public String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Override
  protected void prepareTestAppendedGroups(String username, boolean enabled, String email, String displayName, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + username)
            .build(),
        enabled,
        email,
        displayName);
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + username + "/groups")
            .build(),
        groups);
  }
}
