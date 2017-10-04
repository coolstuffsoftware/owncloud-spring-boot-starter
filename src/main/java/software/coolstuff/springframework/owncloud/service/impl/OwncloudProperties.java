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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.logging.LogLevel;
import org.springframework.validation.annotation.Validated;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Owncloud Configuration Properties.
 *
 * The Configuration Properties will be implemented by 2 Classes
 * <ul>
 *   <li><code>software.coolstuff.springframework.owncloud.service.impl.resource.OwncloudResourceProperties</code><br/>
 *       Implements Resource specific Properties
 *   </li>
 *   <li><code>software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties</code><br/>
 *       Implements REST specific Properties
 *   </li>
 * </ul>
 *
 * Within this Class you find the common Properties used by both Implementations
 *
 * @author mufasa1976
 * @since 1.0.0
 */
@Validated
@Getter
@Setter
public abstract class OwncloudProperties {

  /**
   * Default Buffer Size
   * @since 1.2.0
   */
  public static final int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Common Properties for the UserService
   *
   * @author mufasa1976
   * @since 1.2.0
   */
  @Validated
  @Getter
  @Setter
  public static class UserService {

    /**
     * Should Modifications through {@link OwncloudUserService} be allowed
     * @since 1.2.0
     */
    private boolean enableModifications = false;

  }

  /**
   * Common Resource Service Properties
   *
   * @author mufasa1976
   * @since 1.2.0
   */
  @Validated
  @Getter
  @Setter
  public static class ResourceServiceProperties {

    /**
     * Add &quot;..&quot; to the List of returned OwncloudResources (on list-Methods)
     * @since 1.2.0
     */
    private boolean addRelativeDownPath = true;

    /**
     * Buffer Size for the Copy Process
     * @since 1.2.0
     */
    @Min(1)
    private int pipedStreamBufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * Log-Level of any uncaught Exception while running the Background-Thread of the PipedStream-Synchronizer.
     * @since 1.2.0
     */
    @NotNull
    private LogLevel pipedStreamUncaughtExceptionLogLevel = LogLevel.ERROR;
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
   * @since 1.0.0
   */
  private String location;

  /**
   * User Service common Properties
   * @since 1.2.0
   */
  private UserService userService = new UserService();

  /**
   * WebDAV common Properties
   * @since 1.2.0
   */
  private ResourceServiceProperties resourceService = new ResourceServiceProperties();
}
