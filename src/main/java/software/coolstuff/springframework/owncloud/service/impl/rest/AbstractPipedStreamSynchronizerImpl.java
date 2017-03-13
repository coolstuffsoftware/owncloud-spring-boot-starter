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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourcePipeSynchronizationException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties.ResourceServiceProperties;

@Slf4j
abstract class AbstractPipedStreamSynchronizerImpl {

  private static final int DEFAULT_BUFFER_SIZE = 2048;

  @FunctionalInterface
  static interface VoidResponseExtractor {
    void extractData(ClientHttpResponse clientHttpResponse) throws IOException;
  }

  @FunctionalInterface
  static interface ConsumerWithoutArgument {
    void apply();
  }

  private final Authentication authentication;
  private final OwncloudFileResource owncloudFileResource;
  private final OwncloudRestProperties owncloudRestProperties;
  private final RestOperations restOperations;
  private final Optional<BiFunction<URI, String, URI>> uriResolver;

  private Thread thread;
  @Getter(AccessLevel.PROTECTED)
  private boolean interrupted;
  private final CyclicBarrier pipeSync = new CyclicBarrier(2);

  protected AbstractPipedStreamSynchronizerImpl(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    this.authentication = authentication;
    this.owncloudFileResource = owncloudFileResource;
    this.owncloudRestProperties = owncloudRestProperties;
    this.restOperations = restOperations;
    this.uriResolver = Optional.ofNullable(uriResolver);
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

  protected String getUsername() {
    return authentication.getName();
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

  private void startBackgroundThread() {
    this.interrupted = false;
    thread = new Thread(this::createPipedStream);
    thread.setName(getHttpMethod() + " " + getResolvedURI());
    thread.setUncaughtExceptionHandler(this::handleUncaughtException);
    log.debug("Start the Background Thread {}", thread.getName());
    thread.start();
  }

  private boolean isThreadExistsAndIsAlive() {
    return thread != null && thread.isAlive();
  }

  protected abstract void createPipedStream();

  protected abstract HttpMethod getHttpMethod();

  protected URI getResolvedURI() {
    URI unresolvedUri = getUnresolvedUri();
    return uriResolver
        .map(resolver -> resolver.apply(unresolvedUri, authentication.getName()))
        .orElse(unresolvedUri);
  }

  protected URI getUnresolvedUri() {
    return owncloudFileResource.getHref();
  }

  private void handleUncaughtException(Thread thread, Throwable cause) {
    log.error("Error while executing " + thread.getName(), cause);
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

  protected void execute(ExecutionEnvironment executionEnvironment) {
    Validate.notNull(executionEnvironment);
    Optional<ConsumerWithoutArgument> afterExecutionCallback = executionEnvironment.getAfterExecutionCallback();
    try {
      RequestCallback requestCallback = executionEnvironment.getRequestCallback();
      VoidResponseExtractor responseExtractor = executionEnvironment.getResponseExtractor();
      URI uri = getResolvedURI();
      HttpMethod httpMethod = getHttpMethod();
      log.debug("Execute {} on {}", httpMethod, uri);
      restOperations.execute(
          uri,
          httpMethod,
          clientHttpRequest -> wrapRequestCallback(clientHttpRequest, requestCallback),
          response -> {
            if (responseExtractor != null) {
              responseExtractor.extractData(response);
            }
            return null;
          });
    } catch (RestClientException restClientException) {
      Optional<Consumer<RestClientException>> restClientExceptionHandler = executionEnvironment.getRestClientExceptionHandler();
      restClientExceptionHandler.ifPresent(consumer -> consumer.accept(restClientException));
      throw restClientException;
    } finally {
      afterExecutionCallback.ifPresent(consumer -> consumer.apply());
    }
  }

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  static class ExecutionEnvironment {
    private final RequestCallback requestCallback;
    private VoidResponseExtractor responseExtractor;
    private Consumer<RestClientException> restClientExceptionHandler;
    private ConsumerWithoutArgument afterExecutionCallback;

    public Optional<Consumer<RestClientException>> getRestClientExceptionHandler() {
      return Optional.ofNullable(restClientExceptionHandler);
    }

    public Optional<ConsumerWithoutArgument> getAfterExecutionCallback() {
      return Optional.ofNullable(afterExecutionCallback);
    }
  }

  private void wrapRequestCallback(ClientHttpRequest clientHttpRequest, RequestCallback requestCallback) throws IOException {
    OwncloudRestUtils.addAuthorizationHeader(clientHttpRequest.getHeaders(), authentication);
    addKeepAliveConnectionHeader(clientHttpRequest);
    if (requestCallback != null) {
      requestCallback.doWithRequest(clientHttpRequest);
    }
  }

  private void addKeepAliveConnectionHeader(ClientHttpRequest clientHttpRequest) {
    log.debug("Set the Connection Header to keep-alive");
    HttpHeaders headers = clientHttpRequest.getHeaders();
    headers.add(HttpHeaders.CONNECTION, "keep-alive");
  }

  protected void addContentTypeHeader(ClientHttpRequest clientHttpRequest) {
    MediaType mediaType = owncloudFileResource.getMediaType();
    if (mediaType == null) {
      return;
    }

    log.debug("Set the ContentType Header to {}", mediaType.toString());
    HttpHeaders headers = clientHttpRequest.getHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, mediaType.toString());
  }

  protected void copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[getBufferSize()];
    for (int length = 0; (length = input.read(buffer)) != -1;) {
      output.write(buffer, 0, length);
      if (isInterrupted()) {
        log.warn("Background Thread has been interrupted -> stop the Copy Process");
        return;
      }
    }
  }

  protected final int getBufferSize() {
    return Optional.ofNullable(owncloudRestProperties)
        .map(this::extractBufferSize)
        .orElse(DEFAULT_BUFFER_SIZE);
  }

  private int extractBufferSize(OwncloudRestProperties properties) {
    ResourceServiceProperties resourceProperties = owncloudRestProperties.getResourceService();
    return resourceProperties.getPipedStreamBufferSize();
  }
}
