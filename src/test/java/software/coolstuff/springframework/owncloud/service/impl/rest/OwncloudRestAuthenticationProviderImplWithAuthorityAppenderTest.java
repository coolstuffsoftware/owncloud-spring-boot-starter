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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.ActiveProfiles;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudAuthenticationProviderWithAuthorityAppenderTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

import static org.springframework.http.HttpMethod.GET;

@ActiveProfiles("REST-AUTHORITY-APPENDER-USER-SERVICE")
public class OwncloudRestAuthenticationProviderImplWithAuthorityAppenderTest extends AbstractOwncloudAuthenticationProviderWithAuthorityAppenderTest implements OwncloudRestServiceTest {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  public OwncloudRestService owncloudService() {
    return (OwncloudRestService) authenticationProvider;
  }

  @Override
  protected void prepareTestAppendedGroups(String username, UserResponse userResponse, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
                   .method(GET)
                   .url("/cloud/users/" + username)
                   .build(),
        userResponse);
    respondGroups(
        RestRequest.builder()
                   .server(createServer((OwncloudRestService) userDetailsService))
                   .method(GET)
                   .url("/cloud/users/" + username + "/groups")
                   .build(),
        groups);
  }
}
