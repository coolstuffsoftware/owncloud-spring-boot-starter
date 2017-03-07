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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
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

import lombok.Builder;
import lombok.Getter;
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceChecksumServiceException;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        OwncloudLocalResourceChecksumServiceTest.BeanConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudLocalResourceServiceCopyWebdavDirectoryTestExecutionListener.class
})
@ActiveProfiles("LOCAL-CHECKSUM-SERVICE-READONLY")
public class OwncloudLocalResourceChecksumServiceTest {

  private static final String FILE_CONTENT = "This is just another Test Content";

  private final MessageDigest messageDigest;
  private final ChecksumFileVisitor fileVisitor;

  @Autowired
  private OwncloudLocalResourceChecksumServiceTestImpl checksumService;

  @Autowired
  private OwncloudLocalProperties properties;

  public OwncloudLocalResourceChecksumServiceTest() throws Exception {
    messageDigest = MessageDigest.getInstance("MD5");
    fileVisitor = ChecksumFileVisitor.builder()
        .directoryDigest(this::calculateDirectoryChecksum)
        .fileDigest(this::calculateFileChecksum)
        .build();
  }

  @Before
  public void setUp() throws Exception {
    checksumService.reinitializeBeforeTest();
  }

  @Test
  public void testChecksumOfRootDirectory() throws Exception {
    Path rootDirectory = resolvePath(null);
    String expectedChecksum = calculateChecksum(rootDirectory);
    Optional<String> checksum = checksumService.getChecksum(rootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo(expectedChecksum);
  }

  private Path resolvePath(Path path) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path location = resourceProperties.getLocation();
    if (path == null) {
      return location;
    }
    return location.resolve(path);
  }

  private String calculateChecksum(Path path) throws Exception {
    if (Files.isDirectory(path)) {
      fileVisitor.clearChecksums();
      Files.walkFileTree(path, fileVisitor);
      return fileVisitor.getChecksum(path).orElse(null);
    }
    return calculateFileChecksum(path);
  }

  private String calculateDirectoryChecksum(Path path, Map<Path, String> fileChecksums) {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      fileChecksums.entrySet().stream()
          .filter(entry -> isSameFile(path, entry.getKey().getParent()))
          .forEach(entry -> addChecksum(entry.getValue(), stream));
      synchronized (messageDigest) {
        messageDigest.reset();
        messageDigest.update(stream.toByteArray());
        return Hex.encodeHexString(messageDigest.digest());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isSameFile(Path source, Path destination) {
    try {
      return Files.isSameFile(source, destination);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addChecksum(String checksum, OutputStream stream) {
    try {
      stream.write(checksum.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String calculateFileChecksum(Path path) {
    synchronized (messageDigest) {
      messageDigest.reset();
      try (InputStream input = Files.newInputStream(path)) {
        messageDigest.update(IOUtils.toByteArray(input));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return Hex.encodeHexString(messageDigest.digest());
    }
  }

  @Test
  public void testChecksumOfUserRootDirectory_OK() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user1"));
    String expectedChecksum = calculateChecksum(userRootDirectory);

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo(expectedChecksum);
  }

  @Test
  public void testChecksumOfUserRootDirectory_Empty() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("user2"));
    String expectedChecksum = calculateChecksum(userRootDirectory);

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo(expectedChecksum);
  }

  @Test
  public void testChecksumOfUserRootDirectory_NotExists() throws Exception {
    Path userRootDirectory = resolvePath(Paths.get("unknownUser"));

    Optional<String> checksum = checksumService.getChecksum(userRootDirectory);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isFalse();
  }

  @Test
  public void testChecksumOfFileInUserRootDirectory() throws Exception {
    Path file = resolvePath(Paths.get("user1", "fileInRootDirectory.txt"));
    String expectedChecksum = calculateChecksum(file);

    Optional<String> checksum = checksumService.getChecksum(file);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo(expectedChecksum);
  }

  @Test
  public void testChecksumOfFileInDirectory() throws Exception {
    Path file = resolvePath(Paths.get("user1", "directory", "fileInDirectory.txt"));
    String expectedChecksum = calculateChecksum(file);

    Optional<String> checksum = checksumService.getChecksum(file);
    assertThat(checksum).isNotNull();
    assertThat(checksum.isPresent()).isTrue();
    assertThat(checksum.get()).isEqualTo(expectedChecksum);
  }

  @Test
  public void testChecksumOfNewFileInDirectory() throws Exception {
    Path file = resolvePath(Paths.get("user1", "directory", "secondFileInDirectory.txt"));
    String expectedDirectoryChecksumBeforeFileAdded = calculateChecksum(file.getParent());
    try (Writer writer = Files.newBufferedWriter(file)) {
      IOUtils.write(FILE_CONTENT, writer);
    }
    String expectedFileChecksum = calculateChecksum(file);
    String expectedDirectoryChecksumAfterFileAdded = calculateChecksum(file.getParent());

    Optional<String> checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.isPresent()).isFalse();

    Optional<String> checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedDirectoryChecksumBeforeFileAdded);

    checksumService.recalculateChecksum(file);
    checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.isPresent()).isTrue();
    assertThat(checksumOfFile.get()).isEqualTo(expectedFileChecksum);

    checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedDirectoryChecksumAfterFileAdded);
  }

  @Test(expected = OwncloudLocalResourceChecksumServiceException.class)
  public void testRecalculateChecksumOnMissingFile() throws Exception {
    Path file = resolvePath(Paths.get("user1", "directory", "notExists.txt"));

    checksumService.recalculateChecksum(file);
  }

  @Test
  public void testChecksumOfRemovedFileInDirectory() throws Exception {
    Path file = resolvePath(Paths.get("user1", "directory", "fileInDirectory.txt"));
    String expectedFileChecksum = calculateChecksum(file);
    String expectedDirectoryChecksumBeforeFileRemoved = calculateChecksum(file.getParent());
    Files.delete(file);
    String expectedDirectoryChecksumAfterFileRemoved = calculateChecksum(file.getParent());

    assertThat(expectedDirectoryChecksumBeforeFileRemoved).isNotEqualTo(expectedDirectoryChecksumAfterFileRemoved);

    Optional<String> checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.isPresent()).isTrue();
    assertThat(checksumOfFile.get()).isEqualTo(expectedFileChecksum);

    Optional<String> checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedDirectoryChecksumBeforeFileRemoved);

    checksumService.recalculateChecksum(file.getParent());

    checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.isPresent()).isFalse();

    checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedDirectoryChecksumAfterFileRemoved);
  }

  @Test
  public void testRecalculateChecksums() throws Exception {
    Path file = resolvePath(Paths.get("user1", "directory", "fileInDirectory.txt"));
    String expectedFileChecksumBeforeChanges = calculateChecksum(file);
    String expectedParentDirectoryChecksumBeforeChanges = calculateChecksum(file.getParent());

    try (Writer writer = Files.newBufferedWriter(file)) {
      IOUtils.write("And now there is a completly different Input", writer);
    }
    String expectedFileChecksumAfterChanges = calculateChecksum(file);
    String expectedParentDirectoryChecksumAfterChanges = calculateChecksum(file.getParent());

    assertThat(expectedFileChecksumBeforeChanges).isNotEqualTo(expectedFileChecksumAfterChanges);
    assertThat(expectedParentDirectoryChecksumBeforeChanges).isNotEqualTo(expectedParentDirectoryChecksumAfterChanges);

    Optional<String> checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.isPresent()).isTrue();
    assertThat(checksumOfFile.get()).isEqualTo(expectedFileChecksumBeforeChanges);

    Optional<String> checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedParentDirectoryChecksumBeforeChanges);

    checksumService.recalculateChecksums();

    checksumOfFile = checksumService.getChecksum(file);
    assertThat(checksumOfFile.get()).isEqualTo(expectedFileChecksumAfterChanges);

    checksumOfParentDirectory = checksumService.getChecksum(file.getParent());
    assertThat(checksumOfParentDirectory.get()).isEqualTo(expectedParentDirectoryChecksumAfterChanges);
  }

  @Configuration
  @EnableConfigurationProperties(OwncloudLocalProperties.class)
  @IgnoreOnComponentScan
  public static class BeanConfiguration {
    @Bean
    public OwncloudLocalResourceChecksumService checksumService() throws InstantiationException, IllegalAccessException {
      return new OwncloudLocalResourceChecksumServiceTestImpl();
    }
  }

  private static class OwncloudLocalResourceChecksumServiceTestImpl extends OwncloudLocalResourceChecksumServiceImpl {

    @Autowired
    private OwncloudLocalProperties properties;

    private boolean ignoreReinitializeOnFirstTime = true;

    public void reinitializeBeforeTest() throws Exception {
      if (ignoreReinitializeOnFirstTime) {
        ignoreReinitializeOnFirstTime = false;
        return;
      }
      ResourceServiceProperties resourceProperties = properties.getResourceService();
      Files.walkFileTree(resourceProperties.getLocation(), getFileVisitor());
    }
  }

  private static class ChecksumFileVisitor extends SimpleFileVisitor<Path> {

    private final Function<Path, String> fileDigest;
    private final BiFunction<Path, Map<Path, String>, String> directoryDigest;

    @Getter
    private final Map<Path, String> checksums = new ConcurrentHashMap<>();

    @Builder
    private ChecksumFileVisitor(
        final Function<Path, String> fileDigest,
        final BiFunction<Path, Map<Path, String>, String> directoryDigest) {
      this.fileDigest = fileDigest;
      this.directoryDigest = directoryDigest;
    }

    public void clearChecksums() {
      checksums.clear();
    }

    public Optional<String> getChecksum(Path path) {
      return Optional.ofNullable(path)
          .map(p -> checksums.get(p.toAbsolutePath().normalize()));
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      String checksum = fileDigest.apply(file);
      checksums.put(file.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      String checksum = directoryDigest.apply(dir, checksums);
      checksums.put(dir.toAbsolutePath().normalize(), checksum);
      return FileVisitResult.CONTINUE;
    }
  }

}
