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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder) {
    super(builder, false, new OwncloudAuthenticationProviderResponseErrorHandler());
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    if (StringUtils.isBlank(authentication.getName())) {
      throw new BadCredentialsException("empty username");
    }

    if (authentication.getCredentials() == null) {
      throw new BadCredentialsException("empty password");
    }

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    OwncloudUserDetails owncloudUserDetails = null;
    if (isRestAvailable()) {
      Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(username, password), Ocs.User.class, username);
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));
      owncloudUserDetails = userDetailsService.loadPreloadedUserByUsername(username, user);
    } else {
      if (!resourceService.authenticate(username, password)) {
        throw new BadCredentialsException("wrong password or user not found");
      }
      owncloudUserDetails = userDetailsService.loadUserByUsernameFromResourceService(username);
    }
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  protected void checkFailure(String uri, Ocs.Meta metaInformation) throws OwncloudStatusException {
    if ("ok".equals(metaInformation.getStatus())) {
      return;
    }
    throw new BadCredentialsException(metaInformation.getMessage());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) || OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

  @NoArgsConstructor
  private static class OwncloudAuthenticationProviderResponseErrorHandler extends DefaultOwncloudResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      HttpStatus statusCode = response.getStatusCode();
      if (HttpStatus.UNAUTHORIZED.compareTo(statusCode) == 0) {
        throw new BadCredentialsException("User not found or wrong password");
      }
      super.handleError(response);
    }

  }
}
