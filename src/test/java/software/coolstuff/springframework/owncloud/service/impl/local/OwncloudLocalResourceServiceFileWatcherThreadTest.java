/*
   Copyright (C) 2016 by the original Authors.

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
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author mufasa1976
 */
@Slf4j
public class OwncloudLocalResourceServiceFileWatcherThreadTest {

  @Before
  public void setUp() throws Exception {
    Path testDirectory = getTestDirectory();
    removeTestDirectory(testDirectory);
    createTestDirectoryIfNotExists(testDirectory);
  }

  private Path getTestDirectory() {
    Path testDirectoryPath = Paths.get(".", "target", "fileWatcherTest");
    return testDirectoryPath;
  }

  private void createTestDirectoryIfNotExists(Path path) throws IOException {
    if (Files.notExists(path)) {
      log.debug("Create Directory {}", path);
      Files.createDirectories(path);
    }
  }

  @After
  public void tearDown() throws Exception {
    Path testDirectory = getTestDirectory();
    removeTestDirectory(testDirectory);
  }

  private void removeTestDirectory(Path path) throws IOException {
    if (Files.exists(path)) {
      log.debug("Remove Directory {}", path);
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }

      });
    }
  }

  @Test
  public void testOK() throws Exception {
    MessageDigest messageDigest = getMessageDigest();
    String existingFileName = "existingFile.txt";
    String checksumExistingFile = write(existingFileName, "This is a Content", messageDigest);
    val fileWatcher = OwncloudLocalResourceServiceFileWatcherThread.builder()
        .rootPath(getTestDirectory())
        .messageDigest(messageDigest)
        .build();
    fileWatcher.setPollTimeout(5, TimeUnit.SECONDS);
    fileWatcher.register(new OwncloudLocalResourceFileWatcherListener() {

      @Override
      public List<WatchEvent> getWatchEvents() {
        return Lists.newArrayList(WatchEvent.INITIALIZED);
      }

      @Override
      public void notifyListener() {
        String checksum = fileWatcher.getChecksum(Paths.get(existingFileName));
        assertThat(checksum).isEqualTo(checksumExistingFile);

        try {
          String checksumNewFile = write("newFile.txt", "This is the Content of a new File", messageDigest);
          Thread.sleep(1000);
          checksum = fileWatcher.getChecksum(Paths.get("newFile.txt"));
          assertThat(checksum).isEqualTo(checksumNewFile);
        } catch (IOException e) {
          fail("This should not be thrown", e);
        } catch (InterruptedException e) {
          fail("This should not be thrown", e);
        }

      }

    });
    Thread thread = new Thread(fileWatcher);
    thread.start();
    Thread.sleep(5000);
    fileWatcher.stopWatchService();
  }

  private MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
    return MessageDigest.getInstance("MD5");
  }

  private String write(String fileName, String content, MessageDigest messageDigest) throws IOException {
    Path path = getTestDirectory().resolve(fileName);
    try (Writer writer = new BufferedWriter(new FileWriter(path.toFile()))) {
      IOUtils.write(content, writer);
    }
    try (InputStream stream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
      messageDigest.reset();
      byte[] buffer = IOUtils.toByteArray(stream);
      messageDigest.update(buffer);
      return Hex.encodeHexString(messageDigest.digest());
    }
  }

}
