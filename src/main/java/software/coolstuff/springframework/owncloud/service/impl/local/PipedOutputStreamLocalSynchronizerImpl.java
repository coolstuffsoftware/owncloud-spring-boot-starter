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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudNoFileResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.service.impl.AbstractPipedStreamSynchronizerImpl;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
class PipedOutputStreamLocalSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl implements PipedOutputStreamLocalSynchronizer {

  private final Path outputFile;
  private final OwncloudLocalProperties owncloudLocalProperties;
  private final Optional<Consumer<PipedOutputStreamAfterCopyEnvironment>> afterCopyCallback;

  private Path temporaryFile;
  private SynchronizedPipedOutputStream pipedOutputStream;

  private PipedOutputStreamAfterCopyEnvironment afterCopyCallbackEnvironment;

  private PipedOutputStreamLocalSynchronizerImpl(
      final Authentication authentication,
      final URI uri,
      final Function<URI, Path> uriResolver,
      final OwncloudLocalProperties owncloudLocalProperties,
      final Consumer<PipedOutputStreamAfterCopyEnvironment> afterCopyCallback) {
    super(authentication, owncloudLocalProperties, uri);
    this.outputFile = getOutputFile(uri, uriResolver);
    this.owncloudLocalProperties = owncloudLocalProperties;
    this.afterCopyCallback = Optional.ofNullable(afterCopyCallback);
    afterCopyCallbackEnvironment = PipedOutputStreamAfterCopyEnvironment.builder()
                                                                        .path(outputFile)
                                                                        .uri(getUri())
                                                                        .username(getUsername())
                                                                        .build();
  }

  private Path getOutputFile(URI uri, Function<URI, Path> uriResolver) {
    log.debug("Resolve Path from URI {}", uri);
    Path path = uriResolver.apply(uri);
    checkIsDirectory(path, uri);
    return path;
  }

  private void checkIsDirectory(Path path, URI uri) {
    log.debug("Check if Path {} is a Directory", path.toAbsolutePath().normalize());
    if (Files.isDirectory(path)) {
      log.error("Cannot get OutputStream on Directory {}", path.toAbsolutePath().normalize());
      throw new OwncloudNoFileResourceException(uri);
    }
  }

  @Builder(builderClassName = "PipedOutputStreamLocalSynchronizerBuilder")
  private static PipedOutputStreamLocalSynchronizer build(
      final Authentication authentication,
      final URI uri,
      final Function<URI, Path> uriResolver,
      final OwncloudLocalProperties owncloudLocalProperties,
      final Consumer<PipedOutputStreamAfterCopyEnvironment> afterCopyCallback) {
    return new PipedOutputStreamLocalSynchronizerImpl(
        authentication,
        uri,
        uriResolver,
        owncloudLocalProperties,
        afterCopyCallback);
  }

  @Override
  protected String getThreadName() {
    return "pipe Content of " + outputFile.toAbsolutePath().normalize().toString();
  }

  @Override
  public OutputStream getOutputStream() {
    createTemporaryFile();
    preparePipedOutputStream();
    startThreadAndWaitForConnectedPipe();
    return pipedOutputStream;
  }

  private void createTemporaryFile() {
    ResourceServiceProperties resourceProperties = owncloudLocalProperties.getResourceService();
    String temporaryFilePrefix = resourceProperties.getPipedStreamTemporaryFilePrefix();
    try {
      log.debug("Create a temporary File with Prefix {}", temporaryFilePrefix);
      temporaryFile = Files.createTempFile(temporaryFilePrefix, null);
    } catch (IOException e) {
      val logMessage = String.format("Cannot create temporary File with Prefix %s", temporaryFilePrefix);
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  private void preparePipedOutputStream() {
    pipedOutputStream = new SynchronizedPipedOutputStream(temporaryFile);
  }

  @Override
  protected void createPipedStream() {
    try (InputStream input = new PipedInputStream(pipedOutputStream);
        OutputStream output = Files.newOutputStream(temporaryFile)) {
      setPipeReady();
      long contentLength = copy(input, output);
      afterCopyCallbackEnvironment.setContentLength(contentLength);
    } catch (OwncloudResourceException e) {
      pipedOutputStream.setOwncloudResourceException(e);
      throw e;
    } catch (IOException e) {
      pipedOutputStream.setIOException(e);
      throw new OwncloudLocalResourceException(e);
    } finally {
      try {
        handleFiles();
        afterCopyCallback.ifPresent(consumer -> {
          log.debug("Reinforcement after the Stream has been closed and the temporary File has been moved");
          consumer.accept(afterCopyCallbackEnvironment);
        });
      } catch (OwncloudResourceException e) {
        pipedOutputStream.setOwncloudResourceException(e);
      } finally {
        setPipeReady();
      }
    }
  }

  private void handleFiles() {
    if (pipedOutputStream.isExceptionAvailable()) {
      removeFiles();
      return;
    }
    moveTemporaryFile();
  }

  private void removeFiles() {
    removeFile(temporaryFile, "temporary File");
    removeFile(outputFile, "regular Output File");
  }

  private void removeFile(Path path, String fileType) {
    try {
      log.debug("Remove File {}", path.toAbsolutePath().normalize());
      Files.delete(path);
    } catch (IOException e) {
      log.error(String.format("Cannot remove %s %s", fileType, path.toAbsolutePath().normalize()), e);
      throw new OwncloudLocalResourceException("Error while removing " + fileType, e);
    }
  }

  private void moveTemporaryFile() {
    try {
      log.debug("Move temporary File {} to {}", temporaryFile.toAbsolutePath().normalize(), outputFile.toAbsolutePath().normalize());
      Files.move(temporaryFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      final String logMessage = String.format("Error while moving the temporary File %s to %s",
          temporaryFile.toAbsolutePath(),
          outputFile.toAbsolutePath().normalize());
      log.error(logMessage, e);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Setter
  @RequiredArgsConstructor
  private class SynchronizedPipedOutputStream extends PipedOutputStream {

    private final Path outputPath;

    private IOException iOException;
    private OwncloudResourceException owncloudResourceException;

    private boolean alreadyClosed = false;

    boolean isExceptionAvailable() {
      return iOException != null || owncloudResourceException != null;
    }

    @Override
    public synchronized void close() throws IOException {
      if (alreadyClosed) {
        log.warn("Piped Output Stream has already been marked as closed");
        return;
      }

      try {
        log.debug("Close the PipedOutputStream of Path {}", outputPath.toAbsolutePath().normalize());
        super.close();
      } finally {
        alreadyClosed = true;
        waitForPipeReady();
      }
      throwExistingIOException();
      throwExistingOwncloudResourceException();
    }

    private void throwExistingIOException() throws IOException {
      if (iOException != null) {
        throw iOException;
      }
    }

    private void throwExistingOwncloudResourceException() {
      if (owncloudResourceException != null) {
        throw owncloudResourceException;
      }
    }
  }
}
