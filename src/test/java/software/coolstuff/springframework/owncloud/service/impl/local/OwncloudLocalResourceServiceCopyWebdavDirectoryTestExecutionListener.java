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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class OwncloudLocalResourceServiceCopyWebdavDirectoryTestExecutionListener extends AbstractOwncloudLocalResourceServiceTestExecutionListener {

  private final static Path COPY_LOCATION = Paths.get("src", "test", "resources", "webdavTest");

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    copyDirectory(testContext);
  }

  private void copyDirectory(TestContext testContext) throws IOException {
    Class<?> testClass = testContext.getTestClass();
    ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
    getResourceLocation(activeProfiles.value())
        .ifPresent(this::copyDirectory);
  }

  private void copyDirectory(Path path) {
    Validate.notNull(COPY_LOCATION);
    try {
      createTargetDirectory(path);
      FileUtils.copyDirectory(COPY_LOCATION.toFile(), path.toFile());
      Path emptyDirectory = path.resolve("user2");
      addOrCheckForEmptyDirectory(emptyDirectory);
    } catch (IOException e) {
      throw new RuntimeException(String.format("IOException while creating Directory %s", path.toAbsolutePath().toString()), e);
    }
  }

  private void createTargetDirectory(Path path) throws IOException {
    if (!Files.exists(path)) {
      log.debug("Create Directory {}", path.toAbsolutePath().toString());
      Files.createDirectories(path);
    } else if (!Files.isDirectory(path)) {
      throw new IllegalStateException(String.format("Path %s exists but is not a Directory", path.toAbsolutePath().toString()));
    }
  }

  private void addOrCheckForEmptyDirectory(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    } else if (!Files.isDirectory(path)) {
      throw new IllegalStateException(String.format("Path %s exists but is not a Directory", path.toAbsolutePath().toString()));
    } else {
      Files.list(path)
           .forEach(p -> {
             throw new IllegalStateException(String.format("Directory %s is not empty", path));
           });
    }
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    copyDirectory(testContext);
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    cleanDirectory(testContext);
  }

  private void cleanDirectory(TestContext testContext) throws IOException {
    Class<?> testClass = testContext.getTestClass();
    ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
    getResourceLocation(activeProfiles.value())
        .ifPresent(this::cleanDirectory);
  }

  private void cleanDirectory(Path directory) {
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
      Files.delete(directory);
    } catch (IOException e) {
      throw new RuntimeException(String.format("IOException while cleaning Directory %s", directory.toAbsolutePath().toString()), e);
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    cleanDirectory(testContext);
  }

}
