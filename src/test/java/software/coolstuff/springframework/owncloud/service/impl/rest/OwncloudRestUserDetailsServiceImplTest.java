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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserDetailsServiceTest;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.springframework.http.HttpMethod.GET;

@ActiveProfiles("REST-USER-SERVICE")
public class OwncloudRestUserDetailsServiceImplTest extends AbstractOwncloudUserDetailsServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private UserDetailsService userDetailsService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) userDetailsService;
  }

  @Override
  protected Class<? extends UserDetailsService> getUserDetailsServiceClass() {
    return OwncloudRestUserDetailsServiceImpl.class;
  }

  @Override
  protected void prepareTestUserDetails_OK(String user, UserResponse userResponse, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user)
            .build(),
        userResponse);
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user + "/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestUserDetails_NotFound() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/unknown")
            .build(),
        998,
        "The requested user could not be found");
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testUserDetails_AccessDenied() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/user1")
            .build(),
        997);

    userDetailsService.loadUserByUsername("user1");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testUserDetails_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/user1")
            .build(),
        999);

    userDetailsService.loadUserByUsername("user1");
  }

  @Test(expected = OwncloudInvalidAuthenticationObjectException.class)
  @WithAnonymousUser
  public void testUserDetails_WrongAuthenticationObject() throws MalformedURLException, IOException {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/user/user1")
            .build(),
        UserResponse.builder()
            .enabled(true)
            .email("user1@example.com")
            .displayname("Mr. User 1")
            .quota(1024L)
            .build());

    userDetailsService.loadUserByUsername("user1");
  }

}
