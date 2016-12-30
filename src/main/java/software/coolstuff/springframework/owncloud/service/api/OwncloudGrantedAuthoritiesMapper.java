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
package software.coolstuff.springframework.owncloud.service.api;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

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
 * {@link OwncloudUserDetails} Object returned by the {@link OwncloudUserDetailsRestService#loadUserByUsername(String)}
 *
 * @author mufasa1976
 * @see GrantedAuthoritiesMapper
 */
@FunctionalInterface
public interface OwncloudGrantedAuthoritiesMapper {
  Collection<? extends GrantedAuthority> mapAuthorities(String username, Collection<? extends GrantedAuthority> grantedAuthorities);
}
