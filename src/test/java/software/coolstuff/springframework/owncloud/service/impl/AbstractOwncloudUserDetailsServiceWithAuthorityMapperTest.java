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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;

@RestClientTest(UserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceWithAuthorityMapperTest extends AbstractOwncloudServiceTest {

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void testGrantedAuthorityAppenderLoadedByApplicationContext() throws Exception {
    applicationContext.getBean(GrantedAuthoritiesMapper.class);
  }

  @Test
  @WithMockUser(username = "test1", password = "password")
  public void testMappedGroups() throws Exception {
    prepareTestMappedGroups(
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

    checkAuthorities(userDetails.getUsername(), userDetails.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestMappedGroups(String username, UserResponse userResponse, String... groups) throws Exception {}

}
