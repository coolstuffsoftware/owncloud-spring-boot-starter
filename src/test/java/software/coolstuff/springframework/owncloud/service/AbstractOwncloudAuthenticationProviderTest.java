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
package software.coolstuff.springframework.owncloud.service;

import lombok.Builder;
import lombok.Data;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@RestClientTest(AuthenticationProvider.class)
public abstract class AbstractOwncloudAuthenticationProviderTest extends AbstractOwncloudServiceTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Test
  public void testCorrectClass() {
    assertThat(authenticationProvider.getClass()).isEqualTo(getAuthenticationProviderClass());
  }

  protected abstract Class<? extends AuthenticationProvider> getAuthenticationProviderClass();

  @Test
  public void testSupportedAuthenticationTokens() {
    assertThat(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    assertThat(authenticationProvider.supports(RememberMeAuthenticationToken.class)).isFalse();
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t") // only used for prepareTestAuthenticate_OK
  public void testAuthenticate_OK() throws Exception {
    Credentials credentials = Credentials.builder().username("user1").password("s3cr3t").build();

    prepareTestAuthenticate_OK(
        credentials,
        UserResponse.builder()
                    .enabled(true)
                    .email("user1@example.com")
                    .displayname("Mr. User 1")
                    .quota(1024L)
                    .build(),
        "group1",
        "group2");

    Authentication authentication = authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
    verifyServer();

    assertThat(authentication).isNotNull();
    assertThat(authentication.getClass()).isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    checkAuthorities(authentication.getName(), authentication.getAuthorities(), "group1", "group2");

    assertThat(authentication.getName()).isEqualTo(credentials.getUsername());
    assertThat(authentication.getCredentials()).isEqualTo(credentials.getPassword());

    assertThat(authentication.getPrincipal()).isNotNull();
    assertThat(authentication.getPrincipal().getClass()).isAssignableFrom(OwncloudUserDetails.class);
    OwncloudUserDetails principal = (OwncloudUserDetails) authentication.getPrincipal();
    assertThat(principal.isEnabled()).isTrue();
    assertThat(principal.getDisplayname()).isEqualTo("Mr. User 1");
    assertThat(principal.getEmail()).isEqualTo("user1@example.com");
    assertThat(principal.getQuota()).isEqualTo(1024);
    assertThat(principal.getAuthorities()).hasSize(2);
  }

  protected void prepareTestAuthenticate_OK(Credentials credentials, UserResponse userResponse, String... groups) throws Exception {
  }

  @Test(expected = BadCredentialsException.class)
  public void testAuthenticate_NOK() throws Exception {
    Credentials credentials = Credentials.builder().username("user1").password("wrongPassword").build();

    prepareTestAuthenticate_NOK(credentials);

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  protected void prepareTestAuthenticate_NOK(Credentials credentials) throws Exception {
  }

  @Test(expected = BadCredentialsException.class)
  public void testAuthentication_NOK_NoUser() throws Exception {
    Credentials credentials = Credentials.builder()
                                         .username(null)
                                         .password(null)
                                         .build();

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  @Test(expected = BadCredentialsException.class)
  public void testAuthentication_NOK_NoPassword() throws Exception {
    Credentials credentials = Credentials.builder()
                                         .username("user1")
                                         .password(null)
                                         .build();

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  @Test(expected = BadCredentialsException.class)
  public void testAuthentication_NOK_UsernameNotFoundException() throws Exception {
    Credentials credentials = Credentials.builder()
                                         .username("unknown")
                                         .password("s3cr3t")
                                         .build();

    prepareTestAuthentication_NOK_UsernameNotFoundException(credentials);

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
  }

  protected void prepareTestAuthentication_NOK_UsernameNotFoundException(Credentials credentials) throws Exception {
  }

  @Test(expected = DisabledException.class)
  public void testAuthentication_NOK_DisabledUser() throws Exception {
    Credentials credentials = Credentials.builder()
                                         .username("user2")
                                         .password("s3cr3t")
                                         .build();

    prepareTestAuthentication_NOK_DisabledUser(credentials);

    authenticationProvider.authenticate(credentials.getUsernamePasswordAuthenticationToken());
    verifyServer();
  }

  protected void prepareTestAuthentication_NOK_DisabledUser(Credentials credentials) throws Exception {
  }

  @Data
  @Builder
  protected static class Credentials {

    private final String username;
    private final String password;

    public String getBasicAuthorizationHeader() {
      return "Basic " + Base64.getEncoder().encodeToString((getUsername() + ":" + getPassword()).getBytes());
    }

    public UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken() {
      return new UsernamePasswordAuthenticationToken(username, password);
    }
  }
}
