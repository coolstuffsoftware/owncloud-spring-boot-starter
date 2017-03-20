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
import java.util.function.BiConsumer;
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
  private final BiConsumer<Authentication, Integer> quotaChecker;
  private final Optional<Consumer<Path>> onCloseCallback;

  private Path temporaryFile;

  private PipedOutputStreamLocalSynchronizerImpl(
      final Authentication authentication,
      final URI uri,
      final Function<URI, Path> uriResolver,
      final OwncloudLocalProperties owncloudLocalProperties,
      final BiConsumer<Authentication, Integer> quotaChecker,
      final Consumer<Path> onCloseCallback) {
    super(authentication, owncloudLocalProperties, uri);
    this.outputFile = getOutputFile(uri, uriResolver);
    this.owncloudLocalProperties = owncloudLocalProperties;
    this.pipedOutputStream = new SynchronizedPipedOutputStream();
    this.quotaChecker = quotaChecker;
    this.onCloseCallback = Optional.ofNullable(onCloseCallback);
  }

  private Path getOutputFile(URI uri, Function<URI, Path> uriResolver) {
    Path path = uriResolver.apply(uri);
    checkIsDirectory(path, uri);
    createFileIfNotExists(path);
    return path;
  }

  private void checkIsDirectory(Path path, URI uri) {
    if (Files.isDirectory(path)) {
      throw new OwncloudNoFileResourceException(uri);
    }
  }

  private void createFileIfNotExists(Path path) {
    try {
      if (Files.notExists(path)) {
        Files.createFile(path);
      }
    } catch (IOException e) {
      String exceptionMessage = "Error while creating empty File " + path.toAbsolutePath().normalize().toString();
      log.error(exceptionMessage, e);
      throw new OwncloudLocalResourceException(exceptionMessage, e);
    }
  }

  @Builder
  private static PipedOutputStreamLocalSynchronizer build(
      final Authentication authentication,
      final URI uri,
      final Function<URI, Path> uriResolver,
      final OwncloudLocalProperties owncloudLocalProperties,
      final BiConsumer<Authentication, Integer> quotaChecker,
      final Consumer<Path> onCloseCallback) {
    return new PipedOutputStreamLocalSynchronizerImpl(
        authentication,
        uri,
        uriResolver,
        owncloudLocalProperties,
        quotaChecker,
        onCloseCallback);
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
      copy(input, output, quotaChecker);
    } catch (OwncloudResourceException e) {
      pipedOutputStream.setOwncloudResourceException(e);
      throw e;
    } catch (IOException e) {
      pipedOutputStream.setIOException(e);
      throw new OwncloudLocalResourceException(e);
    } finally {
      waitForPipeReady();
    }
  }

  private class SynchronizedPipedOutputStream extends PipedOutputStream {

    @Setter
    private IOException iOException;

    private Optional<OwncloudResourceException> owncloudResourceException = Optional.empty();

    public void setOwncloudResourceException(OwncloudResourceException owncloudResourceException) {
      this.owncloudResourceException = Optional.ofNullable(owncloudResourceException);
    }

    @Override
    public void close() throws IOException {
      super.close();
      try {
        moveTemporaryToRealFile();
        onCloseCallback.ifPresent(callback -> callback.accept(outputFile));
        throwOptionalIOException();
        owncloudResourceException.ifPresent(OwncloudResourceException::reThrow);
      } finally {
        setPipeReady();
      }
    }

    private void moveTemporaryToRealFile() throws IOException {
      if (isExceptionAvailable()) {
        return;
      }
      Files.move(temporaryFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isExceptionAvailable() {
      return iOException != null || owncloudResourceException.isPresent();
    }

    private void throwOptionalIOException() throws IOException {
      if (iOException != null) {
        throw iOException;
      }
    }
  }
}
