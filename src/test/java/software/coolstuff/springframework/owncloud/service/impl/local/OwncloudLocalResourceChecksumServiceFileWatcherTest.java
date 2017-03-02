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

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        OwncloudLocalResourceChecksumServiceFileWatcherTest.BeanConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudLocalResourceServiceTestExecutionListener.class
})
@ActiveProfiles("LOCAL-FILEWATCHER-CHECKSUM-SERVICE")
@Slf4j
public class OwncloudLocalResourceChecksumServiceFileWatcherTest {

  @Configuration
  @EnableConfigurationProperties(OwncloudLocalProperties.class)
  static class BeanConfiguration {
    @Bean
    public OwncloudLocalResourceChecksumService checksumService() throws InstantiationException, IllegalAccessException {
      return OwncloudLocalResourceChecksumService.ChecksumServiceStrategy.FILE_WATCHER.newInstance();
    }
  }

  private static final String RUN_TESTS = "testFileWatcherChecksumService";

  @Autowired
  private OwncloudLocalResourceChecksumServiceWithListenerRegistration checksumService;

  @Autowired
  private OwncloudLocalProperties properties;

  @BeforeClass
  public static void beforeClass() {
    String property = System.getProperty(RUN_TESTS, "false");
    Assume.assumeFalse(StringUtils.equalsIgnoreCase(property, "false"));
  }

  @Before
  public void setUp() throws InterruptedException {
    checksumService.clearChangeListener();
    checksumService.clearDeleteListener();
  }

  @Test
  public void test_ChecksumOfRootDirectory_OK() throws Exception {
    Path rootDirectory = resolvePath(null);
    String checksum = checksumService.getChecksum(rootDirectory);
    assertThat(checksum).isNotEmpty();
    assertThat(checksum).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
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
  public void test_ChecksumOfRootDirectory_NotChanged() throws Exception {
    Path rootDirectory = resolvePath(null);
    String checksum = checksumService.getChecksum(rootDirectory);
    assertThat(checksum).isNotEmpty();
    assertThat(checksum).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");

    Path file = rootDirectory.resolve("resource1.txt");
    WaitThread waitThread = registerListenerFor(file);
    createFileResource(file);
    waitThread.waitFor();
    checksum = checksumService.getChecksum(rootDirectory);
    assertThat(checksum).isNotEmpty();
    // the MD5-Checksum of the Root-Directory should never change
    assertThat(checksum).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
  }

  private void createFileResource(Path file) throws IOException {
    log.debug("Create File {}", file.toAbsolutePath());
    try (Writer writer = Files.newBufferedWriter(file)) {
      IOUtils.write("This is a test content", writer);
    }
  }

  private WaitThread registerListenerFor(Path path) {
    WaitThread waitThread = new WaitThread(path);
    waitThread.start();
    Consumer<Path> listener = changedPath -> {
      if (changedPath.toAbsolutePath().normalize().equals(path.toAbsolutePath().normalize())) {
        waitThread.interrupt();
      }
    };
    checksumService.registerChangeListener(listener);
    checksumService.registerDeleteListener(listener);
    return waitThread;
  }

  @Setter
  @Getter
  private static class WaitThread extends Thread {

    private final Path path;
    private String changeListenerId;
    private String deleteListenerId;

    public WaitThread(final Path path) {
      this.path = path;
      setName("Wait for Changes on Path " + path.toAbsolutePath().toString());
    }

    public void waitFor() throws InterruptedException {
      log.debug("Wait for changes on " + getPath().toAbsolutePath().normalize());
      this.join();
    }

    @Override
    public void run() {
      LocalTime startTime = LocalTime.now();
      try {
        for (;;) {
          Thread.sleep(1000);
        }
      } catch (InterruptedException ignored) {
        long duration = LocalTime.now().toNanoOfDay() - startTime.toNanoOfDay();
        duration = Long.divideUnsigned(duration, 1_000_000L); // millisecons
        log.debug("Wait Thread for Path {} has been interrupted after {} milliseconds", path, duration);
      }
    }
  }

  @Test
  public void test_ChecksumOfSubdirectory_OK() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user"));
    log.debug("Create Directory {}", userRootDirectory);
    WaitThread waitThread = registerListenerFor(userRootDirectory);
    Files.createDirectories(userRootDirectory);

    waitThread.waitFor();
    String checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotEmpty();
    assertThat(checksum).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
  }

  @Test
  public void testChecksumOfSubdirectory_CreateOneFile() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user"));
    log.debug("Create Directory {}", userRootDirectory);
    WaitThread waitThread = registerListenerFor(userRootDirectory);
    Files.createDirectories(userRootDirectory);

    waitThread.waitFor();
    String checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotEmpty();
    assertThat(checksum).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");

    Path file = userRootDirectory.resolve("resource1.txt");
    waitThread = registerListenerFor(file);
    createFileResource(file);
    waitThread.waitFor();
    checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotEmpty();
    assertThat(checksum).isNotEqualTo("d41d8cd98f00b204e9800998ecf8427e");
  }

}
