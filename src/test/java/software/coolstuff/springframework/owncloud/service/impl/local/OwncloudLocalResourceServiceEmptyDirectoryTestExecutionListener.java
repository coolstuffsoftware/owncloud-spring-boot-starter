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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author mufasa1976
 */
@Slf4j
public class OwncloudLocalResourceServiceEmptyDirectoryTestExecutionListener extends AbstractOwncloudLocalResourceServiceTestExecutionListener {

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
    getResourceLocation(activeProfiles.value())
        .ifPresent(this::createDirectory);
  }

  private void createDirectory(Path path) {
    try {
      if (!Files.exists(path)) {
        log.debug("Create Directory {}", path.toAbsolutePath().toString());
        Files.createDirectories(path);
      } else if (!Files.isDirectory(path)) {
        throw new IllegalStateException(String.format("Path %s exists but is not a Directory", path.toAbsolutePath().toString()));
      }
    } catch (IOException e) {
      throw new RuntimeException(String.format("IOException while creating Directory %s", path.toAbsolutePath().toString()), e);
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
    getResourceLocation(activeProfiles.value())
        .ifPresent(path -> deleteDirectoryContent(path, false));
  }

  private void deleteDirectoryContent(Path directory, boolean removeRootDirectory) {
    if (!Files.isDirectory(directory)) {
      return;
    }

    try {
      log.debug("Remove all Contents of Directory {}", directory.toAbsolutePath().toString());
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          if (!Files.isSameFile(dir, directory)) {
            Files.delete(dir);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(String.format("IOException while cleaning Directory %s", directory.toAbsolutePath().toString()), e);
    }
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
    getResourceLocation(activeProfiles.value())
        .ifPresent(path -> deleteDirectoryContent(path, true));
  }
}
