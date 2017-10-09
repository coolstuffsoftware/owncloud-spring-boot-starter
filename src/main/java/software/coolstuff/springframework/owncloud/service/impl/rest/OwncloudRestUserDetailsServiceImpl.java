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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

@Slf4j
public class OwncloudRestUserDetailsServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudRestUserDetailsService {

  OwncloudRestUserDetailsServiceImpl(RestTemplateBuilder builder, OwncloudRestProperties properties) {
    super(builder, properties);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Validate.notBlank(username);

    log.debug("Get Information about User {} from the Location {}", username, getLocation());
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    return loadPreloadedUserByUsername(username, user);
  }

  @Override
  public OwncloudUserDetails loadPreloadedUserByUsername(String username, Ocs.User preloadedUser) throws UsernameNotFoundException {
    log.debug("Get Information about the Group Memberships of User {} from the Location {}", username, getLocation());
    Ocs.Groups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return convert(username, preloadedUser, groups);
  }

}
