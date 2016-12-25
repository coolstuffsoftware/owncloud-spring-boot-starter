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

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.exception.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@ActiveProfiles("AUTHENTICATED-USER-URL")
public class OwncloudUserDetailsServiceAuthenticatedUserRestTest extends AbstractOwncloudUserDetailsServiceRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public final String getBasicAuthorizationHeader() {
    return getSecurityContextBasicAuthorizationHeader();
  }

  @Test(expected = OwncloudInvalidAuthenticationObjectException.class)
  @WithAnonymousUser
  public void testUserDetails_WrongAuthenticationObject() throws MalformedURLException, IOException {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/user/user1")
            .build(),
        true,
        "user1@example.com",
        "Mr. User 1");

    userDetailsService.loadUserByUsername("user1");
  }

}
