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
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.Builder;
import lombok.Data;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
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
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
@AutoConfigureWebClient
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
    userService.findOne(username)
               .map(OwncloudModificationUser::of)
               .map(modificationUser -> {
                 modificationUser.setQuota(1024L);
                 return modificationUser;
               })
               .ifPresent(userService::save);
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

    Optional<OwncloudUserDetails> user = userService.findOne(username);
    assertThat(user)
        .isNotNull()
        .isPresent();
    assertThat(user.get().getQuota()).isEqualTo(1024);

    user = user
        .map(OwncloudModificationUser::of)
        .map(modificationUser -> {
          modificationUser.setQuota(2048L);
          return modificationUser;
        })
        .map(userService::save);
    assertThat(user)
        .isNotNull()
        .isPresent();
    assertThat(user.get().getQuota()).isEqualTo(2048);

    OwncloudQuota expectedAfterChange = TestOwncloudQuota.builder()
                                                         .username(username)
                                                         .total(2048L)
                                                         .used(97L)
                                                         .free(2048L - 97)
                                                         .relative(97f / 2048f * 100f)
                                                         .build();
    quota = resourceService.getQuota();
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expectedAfterChange, "username", "total", "used", "free", "relative");
  }

}
