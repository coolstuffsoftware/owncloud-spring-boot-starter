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
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
@Slf4j
abstract class AbstractPipedStreamSynchronizerImpl {

  private static final int DEFAULT_BUFFER_SIZE = 2048;

  @FunctionalInterface
  static interface VoidResponseExtractor {
    void extractData(ClientHttpResponse clientHttpResponse) throws IOException;
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

  private final Map<Class<? extends RestClientException>, Consumer<RestClientException>> restClientExceptionHandlers = new HashMap<>();

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
      pipeSync.await();
    } catch (Exception e) {
      log.error("Error while waiting for PipedStream-Barrier", e);
      throw new RuntimeException(e);
    }
  }

  protected final void startThreadAndWaitForConnectedPipe() {
    if (isThreadExistsAndIsAlive()) {
      return;
    }
    this.interrupted = false;
    thread = new Thread(this::createPipedStream);
    thread.setName(getHttpMethod() + " " + getResolvedURI());
    thread.setUncaughtExceptionHandler(this::handleUncaughtException);
    thread.start();
    try {
      pipeSync.await();
    } catch (Exception e) {
      log.error("Error while waiting for PipedStream-Barrier", e);
      throw new RuntimeException(e);
    }
  }

  private boolean isThreadExistsAndIsAlive() {
    return thread != null && thread.isAlive();
  }

  protected abstract void createPipedStream();

  protected abstract HttpMethod getHttpMethod();

  protected URI getResolvedURI() {
    URI href = owncloudFileResource.getHref();
    return uriResolver
        .map(resolver -> resolver.apply(href, authentication.getName()))
        .orElse(href);
  }

  private void handleUncaughtException(Thread thread, Throwable cause) {
    log.error("Error while executing " + thread.getName(), cause);
  }

  protected void execute(
      RequestCallback requestCallback,
      VoidResponseExtractor responseExtractor,
      Consumer<RestClientException> restClientExceptionHandler) {
    if (responseExtractor == null) {
      execute(requestCallback, restClientExceptionHandler);
      return;
    }
    try {
      restOperations.execute(
          getResolvedURI(),
          getHttpMethod(),
          clientHttpRequest -> wrapRequestCallback(clientHttpRequest, requestCallback),
          response -> {
            responseExtractor.extractData(response);
            return null;
          });
    } catch (HttpClientErrorException restClientException) {
      if (restClientExceptionHandler != null) {
        restClientExceptionHandler.accept(restClientException);
      }
      throw restClientException;
    }
  }

  protected void execute(
      RequestCallback requestCallback,
      Consumer<RestClientException> restClientExceptionHandler) {
    try {
      restOperations.execute(
          getResolvedURI(),
          getHttpMethod(),
          clientHttpRequest -> wrapRequestCallback(clientHttpRequest, requestCallback),
          null);
    } catch (RestClientException restClientException) {
      if (restClientExceptionHandler != null) {
        restClientExceptionHandler.accept(restClientException);
      }
      throw restClientException;
    }
  }

  private void wrapRequestCallback(ClientHttpRequest clientHttpRequest, RequestCallback requestCallback) throws IOException {
    addAuthorizationHeader(clientHttpRequest);
    addKeepAliveConnectionHeader(clientHttpRequest);
    if (requestCallback != null) {
      requestCallback.doWithRequest(clientHttpRequest);
    }
  }

  private void addKeepAliveConnectionHeader(ClientHttpRequest clientHttpRequest) {
    HttpHeaders headers = clientHttpRequest.getHeaders();
    headers.add(HttpHeaders.CONNECTION, "keep-alive");
  }

  private void addAuthorizationHeader(ClientHttpRequest clientHttpRequest) {
    HttpHeaders headers = clientHttpRequest.getHeaders();
    if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
      return;
    }
    Encoder base64Encoder = Base64.getEncoder();
    String encodedCredentials = base64Encoder.encodeToString((authentication.getName() + ':' + (String) authentication.getCredentials()).getBytes());
    headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
  }

  protected void copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[getBufferSize()];
    for (int length = 0; (length = input.read(buffer)) != -1;) {
      output.write(buffer, 0, length);
      if (isInterrupted()) {
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

  protected void registerRestClientExceptionHandler(
      Class<? extends RestClientException> restClientExceptionClass,
      Consumer<RestClientException> restClientExceptionHandler) {
    restClientExceptionHandlers.put(restClientExceptionClass, restClientExceptionHandler);
  }

  protected void handleRestClientException(RestClientException restClientException) {
    Consumer<RestClientException> exceptionHandler = getRestClientExceptionHandler(restClientException);
    if (exceptionHandler != null) {
      exceptionHandler.accept(restClientException);
    }
    throw restClientException;
  }

  private Consumer<RestClientException> getRestClientExceptionHandler(RestClientException restClientException) {
    if (restClientException == null) {
      return null;
    }
    Class<? extends RestClientException> restClientExceptionClass = restClientException.getClass();
    return getRestClientExceptionHandler(restClientExceptionClass);
  }

  @SuppressWarnings("unchecked")
  private Consumer<RestClientException> getRestClientExceptionHandler(Class<? extends RestClientException> restClientExceptionClass) {
    if (restClientExceptionClass == RestClientException.class) {
      return null;
    }
    if (restClientExceptionHandlers.containsKey(restClientExceptionClass)) {
      return restClientExceptionHandlers.get(restClientExceptionClass);
    }
    return getRestClientExceptionHandler((Class<? extends RestClientException>) restClientExceptionClass.getSuperclass());
  }

}
