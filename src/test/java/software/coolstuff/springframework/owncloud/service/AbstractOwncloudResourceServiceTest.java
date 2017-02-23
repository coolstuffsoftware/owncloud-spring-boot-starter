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
package software.coolstuff.springframework.owncloud.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        VelocityConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
})
@ComponentScan
@RestClientTest(OwncloudResourceService.class)
public abstract class AbstractOwncloudResourceServiceTest {

  @Autowired
  private OwncloudResourceService resourceService;

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void testImplementationClass() {
    assertThat(resourceService).isNotNull();
    assertThat(resourceService.getClass()).isAssignableFrom(getImplementationClass());
  }

  protected abstract Class<? extends OwncloudResourceService> getImplementationClass();

}
