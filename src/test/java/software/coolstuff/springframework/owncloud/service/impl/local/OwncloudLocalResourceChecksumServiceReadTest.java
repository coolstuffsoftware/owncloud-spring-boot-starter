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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        OwncloudLocalResourceChecksumServiceReadTest.BeanConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class
})
@ActiveProfiles("LOCAL-CHECKSUM-SERVICE-READONLY")
public class OwncloudLocalResourceChecksumServiceReadTest {

  @Configuration
  @EnableConfigurationProperties(OwncloudLocalProperties.class)
  @IgnoreOnComponentScan
  public static class BeanConfiguration {
    @Bean
    public OwncloudLocalResourceChecksumService checksumService() throws InstantiationException, IllegalAccessException {
      return new OwncloudLocalResourceChecksumServiceImpl();
    }
  }

  @Autowired
  private OwncloudLocalResourceChecksumService checksumService;

  @Autowired
  private OwncloudLocalProperties properties;

  @Test
  public void test_ChecksumOfRootDirectory() throws Exception {
    Path rootDirectory = resolvePath(null);
    Optional<String> checksum = checksumService.getChecksum(rootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo("e5f7f88c85d551a5e08a7e10db3651dd");
  }

  private Path resolvePath(Path path) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path location = resourceProperties.getLocation();
    if (path == null) {
      return location;
    }
    return location.resolve(path);
  }

  @Test
  public void test_ChecksumOfUserRootDirectory_OK() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user1"));

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo("c30dd0c9c1fb3846c38aa96cd4bd7513");
  }

  @Test
  @Ignore
  public void test_ChecksumOfUserRootDirectory_Empty() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user2"));

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
  }

  @Test
  public void test_ChecksumOfUserRootDirectory_NotExists() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("unknownUser"));

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isFalse();
  }

  @Test
  public void testChecksumOfFileInUserRootDirectory() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user1", "fileInRootDirectory.txt"));

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo("dc6262f4f09a990bc1d3729ae9ec40d3");
  }

}
