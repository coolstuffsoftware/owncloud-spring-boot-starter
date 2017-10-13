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
package software.coolstuff.springframework.owncloud.exception.resource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;

import java.net.URI;

/**
 * Thrown when the {@link OwncloudResource} couldn't be found
 *
 * @author mufasa1976
 * @since 1.2.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
@RequiredArgsConstructor
public class OwncloudResourceNotFoundException extends OwncloudResourceException {

  private static final long serialVersionUID = 5485512179312773554L;

  @Getter
  private final URI uri;
  private final String username;

  @Override
  public String getMessage() {
    return "Error while accessing Resource " + uri + " by User " + username;
  }

}
