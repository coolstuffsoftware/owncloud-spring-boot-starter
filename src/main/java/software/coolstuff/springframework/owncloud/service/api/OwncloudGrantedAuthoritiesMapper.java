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
package software.coolstuff.springframework.owncloud.service.api;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestUserDetailsServiceImpl;

import java.util.Collection;

/**
 * An Implementation of this Interface can be seen as an Alternative to {@link GrantedAuthoritiesMapper}.
 * <p/>
 * If you want to implement any {@link GrantedAuthority} based on the User then any Implementation of {@link GrantedAuthoritiesMapper}
 * will be of no use because {@link GrantedAuthoritiesMapper#mapAuthorities(Collection)} only gets a Collection of {@link GrantedAuthority}.
 * <p/>
 * By implementing this Interface you also get the Username of the authenticated Owncloud User as a Decision Base for the granted Authorities.
 * So you can implement the following Authentication-Structure (based on Spring Security JDBC):
 * <pre>
 *              +-------------+           +--------------+
 *              | Users       |           | Authorities  |
 *              +-------------+           +--------------+
 *              | PK username |           | PK authority |
 *              +-------------+           +--------------+
 *                     |                          |
 *            +--------+-------+         +--------+-------+
 *           /|\              /|\       /|\              /|\
 *   +-----------------+   +-----------------+   +------------------+
 *   | Memberships     |   | UserAuthorities |   | GroupAuthorities |
 *   +-----------------+   +-----------------+   +------------------+
 *   | PK FK username  |   | PK FK username  |   | PK FK groupname  |
 *   | PK FK groupname |   | PK FK authority |   | PK FK authority  |
 *   +-----------------+   +-----------------+   +------------------+
 *           \|/                                         \|/
 *            +---------------------+---------------------+
 *                                  |
 *                           +--------------+
 *                           | Groups       |
 *                           +--------------+
 *                           | PK groupname |
 *                           +--------------+
 * </pre>
 * Whereas the Tables <code>Users</code>, <code>Groups</code> and <code>Memberships</code> are implemented by Owncloud.
 * <p/>
 * So, in simple Words, by implementing this Interface as a Service you can put the Authorities of your own Application into the
 * {@link OwncloudUserDetails} Object returned by the {@link OwncloudRestUserDetailsServiceImpl#loadUserByUsername(String)}
 *
 * @author mufasa1976
 * @see GrantedAuthoritiesMapper
 * @since 1.0.0
 */
@FunctionalInterface
public interface OwncloudGrantedAuthoritiesMapper {
  Collection<? extends GrantedAuthority> mapAuthorities(String username, Collection<? extends GrantedAuthority> grantedAuthorities);
}
