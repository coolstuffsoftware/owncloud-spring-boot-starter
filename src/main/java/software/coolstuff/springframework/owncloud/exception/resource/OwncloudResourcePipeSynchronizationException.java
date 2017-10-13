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

import software.coolstuff.springframework.owncloud.service.impl.AbstractPipedStreamSynchronizerImpl;

/**
 * Thrown when there was an unexpected Exception during the Pipe Synchronization
 * @see AbstractPipedStreamSynchronizerImpl
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public class OwncloudResourcePipeSynchronizationException extends OwncloudResourceException {

  private static final long serialVersionUID = -8079641711665028483L;

  public OwncloudResourcePipeSynchronizationException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public OwncloudResourcePipeSynchronizationException(Throwable throwable) {
    super(throwable);
  }

}
