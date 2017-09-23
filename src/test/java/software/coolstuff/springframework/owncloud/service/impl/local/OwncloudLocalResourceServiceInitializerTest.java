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

import org.junit.After;
import org.junit.Before;
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
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    properties = "debug=true",
    classes = {
        VelocityConfiguration.class,
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudLocalResourceServiceCopyWebdavDirectoryTestExecutionListener.class
})
@ComponentScan(
    basePackages = "software.coolstuff.springframework.owncloud",
    excludeFilters = @Filter(IgnoreOnComponentScan.class))
@RestClientTest(OwncloudResourceService.class)
@ActiveProfiles("LOCAL-RESOURCE-SERVICE")
public class OwncloudLocalResourceServiceInitializerTest {

  @Autowired
  private OwncloudLocalResourceService resourceService;

  @Autowired
  private OwncloudUserService userService;

  @Before
  public void setUp() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    OwncloudUserDetails user = userService.findOne(username);
    OwncloudModificationUser modificationUser = new OwncloudModificationUser(user);
    modificationUser.setQuota(1024L);
    userService.save(modificationUser);
  }

  @After
  public void tearDown() throws Exception {
    resourceService.recalculateAllUsedSpace();
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getQuota() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudQuota expected = TestOwncloudQuota.builder()
        .username(authentication.getName())
        .total(1024l)
        .used(97l)
        .free(1024l - 97)
        .relative(97f / 1024f * 100f)
        .build();
    OwncloudQuota quota = resourceService.getQuota();
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expected, "username", "total", "used", "free", "relative");
  }

  @Data
  @Builder
  private static class TestOwncloudQuota implements OwncloudQuota {
    private final String username;
    private final long total;
    private final long used;
    private final long free;
    private final float relative;
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_changeQuota_getQuota() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    OwncloudQuota expectedBeforeChange = TestOwncloudQuota.builder()
        .username(username)
        .total(1024l)
        .used(97l)
        .free(1024l - 97)
        .relative(97f / 1024f * 100f)
        .build();

    OwncloudQuota quota = resourceService.getQuota();
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expectedBeforeChange, "username", "total", "used", "free", "relative");

    OwncloudUserDetails user = userService.findOne(username);
    assertThat(user).isNotNull();
    assertThat(user.getQuota()).isEqualTo(1024);

    OwncloudModificationUser modificationUser = new OwncloudModificationUser(user);
    modificationUser.setQuota(2048L);
    user = userService.save(modificationUser);
    assertThat(user).isNotNull();
    assertThat(user.getQuota()).isEqualTo(2048);

    OwncloudQuota expectedAfterChange = TestOwncloudQuota.builder()
        .username(username)
        .total(2048l)
        .used(97l)
        .free(2048l - 97)
        .relative(97f / 2048f * 100f)
        .build();
    quota = resourceService.getQuota();
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expectedAfterChange, "username", "total", "used", "free", "relative");
  }

}
