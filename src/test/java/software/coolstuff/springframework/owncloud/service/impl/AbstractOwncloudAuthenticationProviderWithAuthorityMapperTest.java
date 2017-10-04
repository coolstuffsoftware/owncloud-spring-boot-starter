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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

import static org.assertj.core.api.Assertions.assertThat;

@RestClientTest(UserDetailsService.class)
public abstract class AbstractOwncloudAuthenticationProviderWithAuthorityMapperTest extends AbstractOwncloudServiceTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void testGrantedAuthorityAppenderLoadedByApplicationContext() throws Exception {
    applicationContext.getBean(GrantedAuthoritiesMapper.class);
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t") // only used for prepareTestMappedGroups
  public void testMappedGroups() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "s3cr3t");
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

    authentication = authenticationProvider.authenticate(authentication);
    verifyServer();

    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isInstanceOf(OwncloudUserDetails.class);
    assertThat(authentication.getName()).isEqualTo("user1");

    checkAuthorities(authentication.getName(), authentication.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestMappedGroups(String username, UserResponse userResponse, String... groups) throws Exception {
  }

}
