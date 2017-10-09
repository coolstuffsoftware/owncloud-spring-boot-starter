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

import org.springframework.web.client.RestTemplate;

/**
 * Interface just for make {@link RestTemplate} available within Tests.
 * <p/>
 * Because of AOP we can&apos;t use the abstract Class {@link org.springframework.web.client.support.RestGatewaySupport}
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public interface OwncloudRestService {

  /**
   * get the Rest Template of the Rest Service
   *
   * @return configured Rest Template
   */
  RestTemplate getRestTemplate();

}
