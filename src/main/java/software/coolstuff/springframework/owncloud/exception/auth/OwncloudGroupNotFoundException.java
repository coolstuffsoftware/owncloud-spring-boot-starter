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
package software.coolstuff.springframework.owncloud.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;

/**
 * Thrown when a Owncloud Group doesn&apos;t exist. This is most because of the Membership Maintenance or
 * when a non-existing Group will be removed.
 *
 * @author mufasa1976
 * @since 1.0.0
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Group not found")
public class OwncloudGroupNotFoundException extends OwncloudStatusException {

  private static final long serialVersionUID = -7451968497723587443L;

  public OwncloudGroupNotFoundException(String groupname) {
    super("Group " + groupname + " not found");
  }

}
