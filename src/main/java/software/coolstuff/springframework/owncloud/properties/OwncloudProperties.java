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
package software.coolstuff.springframework.owncloud.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.access.AccessDeniedException;

import lombok.Data;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

/**
 * Spring Boot Configuration Properties for the Owncloud Service.
 *
 * @author mufasa1976
 */
@Data
@ConfigurationProperties(prefix = "owncloud")
public class OwncloudProperties {

  /**
   * Location of the Owncloud Server.
   * 
   * @param The
   *          following Locations are allowed:
   *          <ul>
   *          <li>Address of an Owncloud Server (starts either with <code>http://</code> or <code>https://</code></li>
   *          <li>Classpath Resource (starts with <code>classpath:</code></li>
   *          <li>File Resource outside of the Classpath (start with <code>file:</code></li>
   *          </ul>
   * @return Location of the Owncloud Server
   */
  private String location;

  /**
   * optional Username of the Owncloud Administrator
   * 
   * @param Username
   *          of the Owncloud Administrator
   * @return Username of the Owncloud Administrator
   */
  private String username;

  /**
   * Password of the Owncloud Administrator.
   * <p/>
   * <i>Setting this Property is only neccessary if {@link #username} is used</i>
   * 
   * @param Password
   *          of the Owncloud Administrator
   * @return Password of the Owncloud Administrator
   */
  private String password;

  /**
   * Should User Modifications be allowed by the {@link OwncloudUserModificationService}.
   * <p/>
   * Defaults to <code>false</code>
   * 
   * @param enable
   *          or disable User Modifications
   * @return
   *         <ul>
   *         <li>true ... User Modifications are allowed</li>
   *         <li>false ... User Modifications are disallowed (Exception {@link AccessDeniedException} will be thrown)</li>
   *         <ul>
   */
  private boolean enableModifications = false;

}
