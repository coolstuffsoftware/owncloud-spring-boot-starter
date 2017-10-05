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
package software.coolstuff.springframework.owncloud.service;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

import static org.assertj.core.api.Assertions.assertThat;

@RestClientTest(UserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private UserDetailsService userDetailsService;

  @Test
  public void testCorrectClass() {
    assertThat(AopUtils.getTargetClass(userDetailsService)).isEqualTo(getUserDetailsServiceClass());
  }

  protected abstract Class<? extends UserDetailsService> getUserDetailsServiceClass();

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testUserDetails_OK() throws Exception {
    prepareTestUserDetails_OK(
        "user1",
        UserResponse.builder()
                    .enabled(true)
                    .email("user1@example.com")
                    .displayname("Mr. User 1")
                    .quota(1024L)
                    .build(),
        "group1",
        "group2");

    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");
    verifyServer();

    assertThat(userDetails).isNotNull();

    assertThat(userDetails.getUsername()).isEqualTo("user1");
    assertThat(userDetails.getPassword()).isNull();
    checkAuthorities(userDetails.getUsername(), userDetails.getAuthorities(), "group1", "group2");

    assertThat(userDetails.getClass()).isAssignableFrom(OwncloudUserDetails.class);
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetails;
    assertThat(owncloudUserDetails.getDisplayname()).isEqualTo("Mr. User 1");
    assertThat(owncloudUserDetails.getEmail()).isEqualTo("user1@example.com");
    assertThat(owncloudUserDetails.getQuota()).isEqualTo(1024);
  }

  protected void prepareTestUserDetails_OK(String user, UserResponse userResponse, String... groups) throws Exception {
  }

  @Test(expected = UsernameNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testUserDetails_NotFound() throws Exception {
    prepareTestUserDetails_NotFound();

    userDetailsService.loadUserByUsername("unknown");
  }

  protected void prepareTestUserDetails_NotFound() throws Exception {
  }

  @Test(expected = OwncloudInvalidAuthenticationObjectException.class)
  @WithAnonymousUser
  public void testWrongAuthenticationObject() throws Exception {
    userDetailsService.loadUserByUsername("unknown");
  }
}
