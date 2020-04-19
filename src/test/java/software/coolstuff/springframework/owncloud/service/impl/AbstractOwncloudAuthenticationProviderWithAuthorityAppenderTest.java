/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.impl;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebClient
public abstract class AbstractOwncloudAuthenticationProviderWithAuthorityAppenderTest extends AbstractOwncloudServiceTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void testGrantedAuthorityAppenderLoadedByApplicationContext() throws Exception {
    applicationContext.getBean(OwncloudGrantedAuthoritiesMapper.class);
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t") // only used for prepareTestAppendedGroups
  public void testAppendedGroups() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken("user1", "s3cr3t");
    prepareTestAppendedGroups(
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

    checkAuthorities(authentication.getName(), authentication.getAuthorities(), "group1", "group2", "group98", "group99");
  }

  protected void prepareTestAppendedGroups(String username, UserResponse userResponse, String... groups) throws Exception {
  }

}
