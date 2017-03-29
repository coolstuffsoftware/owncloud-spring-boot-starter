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
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.impl.AbstractPipedStreamSynchronizerImpl;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

@Slf4j
abstract class AbstractPipedStreamRestSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl {

  @FunctionalInterface
  static interface VoidResponseExtractor {
    void extractData(ClientHttpResponse clientHttpResponse) throws IOException;
  }

  @FunctionalInterface
  static interface ConsumerWithoutArgument {
    void apply();
  }

  private final RestOperations restOperations;
  private final Optional<BiFunction<URI, String, URI>> uriResolver;

  protected AbstractPipedStreamRestSynchronizerImpl(
      final Authentication authentication,
      final URI uri,
      final OwncloudProperties owncloudProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    super(authentication, owncloudProperties, uri);
    this.restOperations = restOperations;
    this.uriResolver = Optional.ofNullable(uriResolver);
  }

  @Override
  protected final String getThreadName() {
    return getHttpMethod() + " " + getResolvedURI();
  }

  protected abstract HttpMethod getHttpMethod();

  protected URI getResolvedURI() {
    URI unresolvedUri = getUri();
    return uriResolver
        .map(resolver -> resolver.apply(unresolvedUri, getUsername()))
        .orElse(unresolvedUri);
  }

  protected void execute(ExecutionEnvironment executionEnvironment) {
    Validate.notNull(executionEnvironment);
    Optional<ConsumerWithoutArgument> afterExecutionCallback = executionEnvironment.getAfterExecutionCallback();
    try {
      callRestWith(executionEnvironment);
    } catch (RuntimeException runtimeException) {
      Optional<Consumer<RuntimeException>> runtimeExceptionHandler = executionEnvironment.getRuntimeExceptionHandler();
      runtimeExceptionHandler.ifPresent(consumer -> consumer.accept(runtimeException));
      throw runtimeException;
    } finally {
      afterExecutionCallback.ifPresent(consumer -> consumer.apply());
    }
  }

  private void callRestWith(ExecutionEnvironment executionEnvironment) {
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
  }

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  static class ExecutionEnvironment {
    private final RequestCallback requestCallback;
    private VoidResponseExtractor responseExtractor;
    private Consumer<RuntimeException> runtimeExceptionHandler;
    private ConsumerWithoutArgument afterExecutionCallback;

    public Optional<Consumer<RuntimeException>> getRuntimeExceptionHandler() {
      return Optional.ofNullable(runtimeExceptionHandler);
    }

    public Optional<ConsumerWithoutArgument> getAfterExecutionCallback() {
      return Optional.ofNullable(afterExecutionCallback);
    }
  }

  private void wrapRequestCallback(ClientHttpRequest clientHttpRequest, RequestCallback requestCallback) throws IOException {
    OwncloudRestUtils.addAuthorizationHeader(clientHttpRequest.getHeaders(), getAuthentication());
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

}
