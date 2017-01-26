/*
   Copyright (C) 2017 by the original Authors.

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
package software.coolstuff.springframework.owncloud.service.impl.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

@ActiveProfiles("LOCAL-RESOURCE-SERVICE")
public class OwncloudLocalResourceServiceTest extends AbstractOwncloudResourceServiceTest {

  @Autowired
  private OwncloudResourceService resourceService;

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void testImplementationClass() {
    assertThat(resourceService).isNotNull();
    assertThat(resourceService.getClass()).isAssignableFrom(OwncloudLocalResourceServiceImpl.class);
  }

}
