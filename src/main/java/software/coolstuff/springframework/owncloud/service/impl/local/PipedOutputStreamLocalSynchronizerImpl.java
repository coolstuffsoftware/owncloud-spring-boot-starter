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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.security.core.Authentication;

import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudNoFileResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.service.impl.AbstractPipedStreamSynchronizerImpl;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@Slf4j
class PipedOutputStreamLocalSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl implements PipedOutputStreamLocalSynchronizer {

  private final Path outputFile;
  private final OwncloudLocalProperties owncloudLocalProperties;
  private final SynchronizedPipedOutputStream pipedOutputStream;
  private final Optional<Consumer<PipedOutputStreamAfterCopyEnvironment>> afterCopyCallback;

  private Path temporaryFile;

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
    this.pipedOutputStream = new SynchronizedPipedOutputStream();
    this.afterCopyCallback = Optional.ofNullable(afterCopyCallback);
    afterCopyCallbackEnvironment = PipedOutputStreamAfterCopyEnvironment.builder()
        .path(outputFile)
        .uri(getUri())
        .username(getUsername())
        .build();
  }

  private Path getOutputFile(URI uri, Function<URI, Path> uriResolver) {
    Path path = uriResolver.apply(uri);
    checkIsDirectory(path, uri);
    return path;
  }

  private void checkIsDirectory(Path path, URI uri) {
    if (Files.isDirectory(path)) {
      throw new OwncloudNoFileResourceException(uri);
    }
  }

  @Builder
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
    startThreadAndWaitForConnectedPipe();
    return pipedOutputStream;
  }

  private void createTemporaryFile() {
    try {
      ResourceServiceProperties resourceProperties = owncloudLocalProperties.getResourceService();
      temporaryFile = Files.createTempFile(resourceProperties.getPipedStreamTemporaryFilePrefix(), null);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException(e);
    }
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
        afterCopyCallback.ifPresent(consumer -> consumer.accept(afterCopyCallbackEnvironment));
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
      Files.delete(path);
    } catch (IOException e) {
      throw new OwncloudLocalResourceException("Error while removing " + fileType, e);
    }
  }

  private void moveTemporaryFile() {
    try {
      Files.move(temporaryFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      final String logMessage = String.format("Error while moving the temporary File %s to %s",
          temporaryFile.toAbsolutePath(),
          outputFile.toAbsolutePath().normalize());
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  @Setter
  private class SynchronizedPipedOutputStream extends PipedOutputStream {

    private IOException iOException;
    private OwncloudResourceException owncloudResourceException;

    public boolean isExceptionAvailable() {
      return iOException != null || owncloudResourceException != null;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      } finally {
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
