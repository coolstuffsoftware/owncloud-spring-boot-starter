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
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class OwncloudLocalUserDetailsServiceImpl implements OwncloudUserDetailsService {

  private final OwncloudLocalUserDataService localDataService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Get Information about User {} from the Resource Service", username);
    OwncloudLocalUserData.User user = Optional
        .ofNullable(localDataService.getUser(username))
        .orElseThrow(() -> {
          log.error("User {} not found", username);
          return new UsernameNotFoundException(username);
        });
    return localDataService.convert(user, true);
  }

}
