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

import java.util.concurrent.TimeUnit;

import lombok.Data;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

/**
 * Owncloud Configuration Properties.
 *
 * The Configuration Properties will be implemented by 2 Classes
 * <ul>
 *   <li>software.coolstuff.springframework.owncloud.service.impl.resource.OwncloudResourceProperties<br/>
 *       Implements Resource specific Properties
 *   </li>
 *   <li>software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties<br/>
 *       Implements REST specific Properties
 *   </li>
 * </ul>
 *
 * Within this Class you find the common Properties used by both Implementations
 *
 * @author mufasa1976
 */
@Data
public abstract class OwncloudProperties {

  /**
   * Common Properties for the UserService
   *
   * @author mufasa1976
   */
  @Data
  public static class UserService {

    /**
     * Should Modifications through {@link OwncloudUserModificationService} be allowed
     */
    private boolean enableModifications = false;

  }

  /**
   * Common Resource Service Properties
   *
   * @author mufasa1976
   */
  @Data
  public static class ResourceServiceProperties {

    @Data
    public static class CacheProperties {
      private Integer concurrencyLevel;
      private Long expireAfterAccess;
      private TimeUnit expireAfterAccessTimeUnit = TimeUnit.SECONDS;
      private Long expireAfterWrite;
      private TimeUnit expireAfterWriteTimeUnit = TimeUnit.SECONDS;
      private Integer initialCapacity;
      private Long maximumSize;
      private Long maximumWeight;
      private Long refreshAfterWrite;
      private TimeUnit refreshAfterWriteTimeUnit;
    }

    /**
     * Cache Properties for the Sardine-Cache.
     *
     * The Sardine-Cache will be used to store any created Sardine-Implementation
     * to the authenticated User, which has requested the Sardine-Interface.
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * Should the root of the Search-Path (on list-Methods) be renamed to .
     */
    private boolean renameSearchPathToDot = true;

    /**
     * Add .. to the List of returned OwncloudResources (on list-Methods)
     */
    private boolean addRelativeDownPath = true;

  }

  /**
   * Location of the Owncloud Instance.
   * <p/>
   * May start with either of these:
   * <table>
   *   <tr>
   *     <th>Prefix</th>
   *     <th>Usage</th>
   *   </tr>
   *   <tr>
   *     <td><code>classpath:</code></td>
   *     <td>Use a XML-Resource within the Classpath to configure the Owncloud Services</td>
   *   </tr>
   *   <tr>
   *     <td><code>file:</code></td>
   *     <td>Use a XML-Resource outside the Classpath to configure the Owncloud Services</td>
   *   </tr>
   *   <tr>
   *     <td><code>http://</code> or<br/><code>https://</code></td>
   *     <td>Connects to the Owncloud Instance</td>
   *   </tr>
   * </table>
   */
  private String location;

  /**
   * User Service common Properties
   */
  private UserService userService = new UserService();

  /**
   * WebDAV common Properties
   */
  private ResourceServiceProperties resourceService = new ResourceServiceProperties();

}
