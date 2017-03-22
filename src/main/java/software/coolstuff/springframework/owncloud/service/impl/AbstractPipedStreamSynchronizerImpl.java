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
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.boot.logging.LogLevel;
import org.springframework.security.core.Authentication;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourcePipeSynchronizationException;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
@Slf4j
public abstract class AbstractPipedStreamSynchronizerImpl {

  private static final int EOF = -1;

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
      log.debug("Release the Cyclic Barrier for the synchronized piped Stream");
      pipeSync.await();
    } catch (Exception e) {
      log.error("Error while waiting for PipedStream-Barrier", e);
      throw new OwncloudResourcePipeSynchronizationException(e);
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
    String logMessage = new StringBuilder("Error while executing ")
        .append(thread.getName())
        .toString();
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
      log.debug("Wait for the Background Thread of the synchronized piped Stream");
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

  protected void copy(InputStream input, OutputStream output) throws IOException {
    copy(CopyEnvironment.builder()
        .inputStream(input)
        .outputStream(output)
        .build());
  }

  protected void copy(CopyEnvironment copyEnvironment) throws IOException {
    long sumLength = 0;
    try {
      byte[] buffer = new byte[getBufferSize()];
      InputStream input = copyEnvironment.getInputStream();
      OutputStream output = copyEnvironment.getOutputStream();
      QuotaCheckEnvironment quotaCheckEnvironment = QuotaCheckEnvironment.builder()
          .uri(getUri())
          .username(getUsername())
          .build();
      for (int length = 0; (length = input.read(buffer)) != EOF; sumLength += length) {
        copyEnvironment.checkQuota(quotaCheckEnvironment, length);
        output.write(buffer, 0, length);
        if (isInterrupted()) {
          log.warn("Background Thread has been interrupted -> stop the Copy Process");
          return;
        }
      }
    } catch (Exception e) {
      copyEnvironment.resetQuota(authentication, sumLength);
      throw e;
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  protected static class CopyEnvironment {
    @Getter
    private final InputStream inputStream;
    @Getter
    private final OutputStream outputStream;
    private Consumer<QuotaCheckEnvironment> quotaChecker;
    private BiConsumer<String, Long> quotaResetter;

    public void checkQuota(QuotaCheckEnvironment quotaCheckEnvironment, int length) {
      if (quotaChecker != null) {
        quotaCheckEnvironment.setWrittenBytes(length);
        quotaChecker.accept(quotaCheckEnvironment);
      }
    }

    public void resetQuota(Authentication authentication, long sumLength) {
      if (quotaResetter != null) {
        quotaResetter.accept(authentication.getName(), sumLength);
      }
    }
  }

  private int getBufferSize() {
    return Optional.ofNullable(owncloudProperties)
        .map(this::extractBufferSize)
        .orElse(OwncloudProperties.DEFAULT_BUFFER_SIZE);
  }

  private int extractBufferSize(OwncloudProperties properties) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    return resourceProperties.getPipedStreamBufferSize();
  }

}
