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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceChecksumServiceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.MessageDigestAlgorithm;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@Slf4j
class OwncloudLocalResourceChecksumServiceImpl implements OwncloudLocalResourceChecksumService {

  @Autowired
  private OwncloudLocalProperties properties;

  private final Map<Path, String> checksums = new ConcurrentHashMap<>();

  private final InitializingFileVisitor fileVisitor;
  private MessageDigest messageDigest;

  public OwncloudLocalResourceChecksumServiceImpl() {
    fileVisitor = InitializingFileVisitor.builder()
        .checksums(checksums)
        .directoryDigest(this::createDirectoryChecksum)
        .fileDigest(this::createFileChecksum)
        .build();
  }

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    OwncloudLocalUtils.checkPrivilegesOnDirectory(resourceProperties.getLocation());
    setMessageDigest(resourceProperties);
    Files.walkFileTree(resourceProperties.getLocation(), getFileVisitor());
  }

  protected final FileVisitor<Path> getFileVisitor() {
    return fileVisitor;
  }

  private void setMessageDigest(ResourceServiceProperties resourceProperties) throws NoSuchAlgorithmException {
    MessageDigestAlgorithm messageDigestAlgorithm = resourceProperties.getMessageDigestAlgorithm();
    Validate.notNull(messageDigestAlgorithm);
    messageDigest = messageDigestAlgorithm.getMessageDigest();
  }

  private String createDirectoryChecksum(Path path, Map<Path, String> fileChecksums) {
    log.debug("Calculate the Checksum of Directory {}", path);
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      fileChecksums.entrySet().stream()
          .filter(entry -> isSamePath(path, entry.getKey().getParent()))
          .forEach(entry -> writeChecksumEntry(entry.getValue(), stream));
      synchronized (messageDigest) {
        messageDigest.reset();
        messageDigest.update(stream.toByteArray());
        return Hex.encodeHexString(messageDigest.digest());
      }
    } catch (IOException e) {
      throw new OwncloudLocalResourceChecksumServiceException(e);
    }
  }

  private boolean isSamePath(Path source, Path destination) {
    try {
      return Files.isSameFile(source, destination);
    } catch (IOException e) {
      throw new OwncloudLocalResourceChecksumServiceException(e);
    }
  }

  private void writeChecksumEntry(String checksum, ByteArrayOutputStream stream) {
    try {
      stream.write(checksum.getBytes());
    } catch (IOException e) {
      throw new OwncloudLocalResourceChecksumServiceException(e);
    }
  }

  private String createFileChecksum(Path path) {
    log.debug("Calculate the Checksum of File {}", path);
    try (InputStream stream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
      synchronized (messageDigest) {
        messageDigest.reset();
        byte[] buffer = IOUtils.toByteArray(stream);
        messageDigest.update(buffer);
        return Hex.encodeHexString(messageDigest.digest());
      }
    } catch (IOException e) {
      throw new OwncloudLocalResourceChecksumServiceException(e);
    }
  }

  @Override
  public Optional<String> getChecksum(Path path) throws OwncloudResourceException {
    return Optional.ofNullable(path)
        .map(p -> checksums.get(p.toAbsolutePath().normalize()));
  }

  @Override
  public void recalculateChecksum(Path path) throws OwncloudResourceException {
    Validate.notNull(path);
    if (Files.notExists(path)) {
      checksums.remove(path.toAbsolutePath().normalize());
      return;
    }
    if (Files.isDirectory(path)) {
      createDirectoryChecksumRecursively(path);
      return;
    }
    String checksum = createFileChecksum(path);
    checksums.put(path.toAbsolutePath().normalize(), checksum);
    createDirectoryChecksumRecursively(path.getParent());
  }

  private void createDirectoryChecksumRecursively(Path path) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path rootDirectory = resourceProperties.getLocation().toAbsolutePath().normalize();
    Path normalizedPath = path.toAbsolutePath().normalize();
    if (rootDirectory.equals(normalizedPath)) {
      return;
    }
    checksums.keySet().stream()
        .filter(checksumPath -> isSamePath(checksumPath.getParent(), path))
        .filter(Files::notExists)
        .forEach(checksums::remove);
    String checksum = createDirectoryChecksum(normalizedPath, checksums);
    checksums.put(normalizedPath, checksum);
    createDirectoryChecksumRecursively(normalizedPath.getParent());
  }

  @Override
  public void recalculateChecksums() throws OwncloudResourceException {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    try {
      Files.walkFileTree(resourceProperties.getLocation(), getFileVisitor());
    } catch (IOException e) {
      throw new OwncloudLocalResourceChecksumServiceException(e);
    }
  }

  private static class InitializingFileVisitor extends SimpleFileVisitor<Path> {

    private final Function<Path, String> fileDigest;
    private final BiFunction<Path, Map<Path, String>, String> directoryDigest;

    private final Map<Path, String> checksums;

    @Builder
    private InitializingFileVisitor(
        final Function<Path, String> fileDigest,
        final BiFunction<Path, Map<Path, String>, String> directoryDigest,
        final Map<Path, String> checksums) {
      this.fileDigest = fileDigest;
      this.directoryDigest = directoryDigest;
      this.checksums = checksums;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      checksums.keySet().stream()
          .filter(path -> path.toAbsolutePath().normalize().getParent().equals(dir.toAbsolutePath().normalize()))
          .forEach(path -> checksums.remove(path.toAbsolutePath().normalize()));
      return FileVisitResult.CONTINUE;
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
