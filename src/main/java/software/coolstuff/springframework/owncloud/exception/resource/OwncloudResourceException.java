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

import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

/**
 * Any SubClass of this Exception will be thrown during the Usage
 * of the {@link OwncloudResourceService}
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public abstract class OwncloudResourceException extends RuntimeException {

  private static final long serialVersionUID = -7920186346125342618L;

  protected OwncloudResourceException() {
    super();
  }

  protected OwncloudResourceException(String message) {
    super(message);
  }

  protected OwncloudResourceException(Throwable throwable) {
    super(throwable);
  }

  protected OwncloudResourceException(String message, Throwable throwable) {
    super(message, throwable);
  }

}
