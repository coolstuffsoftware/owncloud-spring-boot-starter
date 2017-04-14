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
package software.coolstuff.springframework.owncloud.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;

import org.springframework.boot.logging.LogLevel;
import org.springframework.security.core.Authentication;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourcePipeSynchronizationException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties.ResourceServiceProperties;

/**
 * Abstract Class of the PipedStream Synchronizer Implementations.
 * <p/>
 * This Class provides only protected Methods used by
 * the PipedStream Synchronizer Implementations.
 * <p/>
 * A PipedStream Synchronizer handles the Synchronization between
 * {@link PipedInputStream} and {@link PipedOutputStream} and acts
 * as a Proxy between the Application which uses the Owncloud API
 * and the Implementation in the Background (either local or REST based).
 * <p/>
 * In fact, the Method {@link OwncloudResourceService#getInputStream(software.coolstuff.springframework.owncloud.model.OwncloudFileResource)}
 * will return a {@link PipedInputStream} whereas the Methods {@link OwncloudResourceService#getOutputStream(software.coolstuff.springframework.owncloud.model.OwncloudFileResource)}
 * and {@link OwncloudResourceService#getOutputStream(URI, org.springframework.http.MediaType)} will return a {@link PipedOutputStream}.
 * <p/>
 * The reverse Part of the returned Stream will be handled by a PipedStream Synchronizer Implementation
 *
 * @author mufasa1976
 * @since 1.2.0
 */
@Slf4j
public abstract class AbstractPipedStreamSynchronizerImpl {

  protected static final int EOF = -1;

  @Getter(AccessLevel.PROTECTED)
  private final Authentication authentication;
  private final OwncloudProperties owncloudProperties;
  private final URI uri;

  private Thread thread;
  @Getter(AccessLevel.PROTECTED)
  private boolean interrupted;
  private final CyclicBarrier pipeSync = new CyclicBarrier(2);

  protected AbstractPipedStreamSynchronizerImpl(
      final Authentication authentication,
      final OwncloudProperties owncloudProperties,
      final URI uri) {
    this.authentication = authentication;
    this.owncloudProperties = owncloudProperties;
    this.uri = uri;
  }

  @Override
  protected void finalize() throws Throwable {
    if (thread != null && thread.isAlive()) {
      log.debug("Stop Thread {}", thread.getName());
      interrupt(thread);
      thread.join();
    }
  }

  private void interrupt(Thread thread) {
    interrupted = true;
    thread.interrupt();
  }

  protected void setPipeReady() {
    try {
      log.debug("Release the Cyclic Barrier for the synchronized piped Stream of Thread {}", Thread.currentThread());
      pipeSync.await();
    } catch (Exception e) {
      final String logMessage = String.format("Error while waiting for the PipedStream-Barrier by Thread %s", Thread.currentThread());
      log.error(logMessage, e);
      throw new OwncloudResourcePipeSynchronizationException(logMessage, e);
    }
  }

  protected final void startThreadAndWaitForConnectedPipe() {
    if (isThreadExistsAndIsAlive()) {
      return;
    }
    startBackgroundThread();
    waitForPipeReady();
  }

  private boolean isThreadExistsAndIsAlive() {
    return thread != null && thread.isAlive();
  }

  private void startBackgroundThread() {
    interrupted = false;
    thread = new Thread(this::createPipedStream);
    thread.setName(getThreadName());
    thread.setUncaughtExceptionHandler(this::handleUncaughtException);
    log.debug("Start the Background Thread {}", thread.getName());
    thread.start();
  }

  protected abstract void createPipedStream();

  protected abstract String getThreadName();

  private void handleUncaughtException(Thread thread, Throwable cause) {
    LogLevel logLevel = getUncaughtExceptionLogLevel();
    if (logLevel == LogLevel.OFF) {
      return;
    }
    final String logMessage = String.format("Error while executing %s", thread);
    switch (logLevel) {
      case TRACE:
        log.trace(logMessage, cause);
        break;
      case DEBUG:
        log.debug(logMessage, cause);
        break;
      case INFO:
        log.info(logMessage, cause);
        break;
      case WARN:
        log.warn(logMessage, cause);
        break;
      default:
        log.error(logMessage, cause);
    }
  }

  private LogLevel getUncaughtExceptionLogLevel() {
    ResourceServiceProperties resourceProperties = owncloudProperties.getResourceService();
    return resourceProperties.getPipedStreamUncaughtExceptionLogLevel();
  }

  protected void waitForPipeReady() {
    try {
      log.debug("Thread {} waits for the Background Thread of the synchronized piped Stream", Thread.currentThread());
      pipeSync.await();
    } catch (Exception e) {
      log.error("Error while waiting for PipedStream-Barrier", e);
      throw new OwncloudResourcePipeSynchronizationException(e);
    }
  }

  protected String getUsername() {
    return authentication.getName();
  }

  protected URI getUri() {
    return uri;
  }

  protected long copy(InputStream input, OutputStream output) throws IOException {
    long contentLength = 0;
    int bufferSize = getBufferSize();
    log.debug("Copy Content with a Buffersize of {} Bytes", bufferSize);
    byte[] buffer = new byte[bufferSize];
    for (int length = 0; (length = input.read(buffer)) != EOF;) {
      output.write(buffer, 0, length);
      contentLength += length;
      if (isInterrupted()) {
        log.warn("Background Thread has been interrupted -> stop the Copy Process");
        return contentLength;
      }
    }
    return contentLength;
  }

  protected int getBufferSize() {
    return Optional.ofNullable(owncloudProperties)
        .map(this::extractBufferSize)
        .orElse(OwncloudProperties.DEFAULT_BUFFER_SIZE);
  }

  private int extractBufferSize(OwncloudProperties properties) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    return resourceProperties.getPipedStreamBufferSize();
  }

}
