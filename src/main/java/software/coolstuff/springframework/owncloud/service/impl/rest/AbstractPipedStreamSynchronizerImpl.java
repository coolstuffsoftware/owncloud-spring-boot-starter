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
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.concurrent.CyclicBarrier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * @author mufasa1976
 */
abstract class AbstractPipedStreamSynchronizerImpl implements PipedStreamSynchronizer {

  @FunctionalInterface
  static interface VoidResponseExtractor {
    void extractData(ClientHttpResponse clientHttpResponse) throws IOException;
  }

  private final URI uri;
  @Getter(AccessLevel.PROTECTED)
  private final Authentication authentication;
  private final RestOperations restOperations;

  private final CyclicBarrier pipeSync = new CyclicBarrier(2);

  protected AbstractPipedStreamSynchronizerImpl(
      final URI uri,
      final Authentication authentication,
      final RestOperations restOperations) {
    this.uri = uri;
    this.authentication = authentication;
    this.restOperations = restOperations;
  }

  protected void setPipeReady() {
    try {
      pipeSync.await();
    } catch (Exception e) {}
  }

  @Override
  public void startAndWaitForConnectedPipe() {
    Thread thread = new Thread(this::createPipedStream);
    thread.setName(getHttpMethod() + " " + uri);
    thread.start();
    try {
      pipeSync.await();
    } catch (Exception e) {}
  }

  protected abstract void createPipedStream();

  protected abstract HttpMethod getHttpMethod();

  protected void execute(RequestCallback requestCallback, VoidResponseExtractor responseExtractor) {
    if (responseExtractor == null) {
      execute(requestCallback);
      return;
    }
    restOperations.execute(
        uri,
        getHttpMethod(),
        clientHttpRequest -> wrapRequestCallback(clientHttpRequest, requestCallback),
        response -> {
          responseExtractor.extractData(response);
          return null;
        });
  }

  protected void execute(RequestCallback requestCallback) {
    restOperations.execute(uri, getHttpMethod(), clientHttpRequest -> wrapRequestCallback(clientHttpRequest, requestCallback), null);
  }

  private void wrapRequestCallback(ClientHttpRequest clientHttpRequest, RequestCallback requestCallback) throws IOException {
    addAuthorizationHeader(clientHttpRequest);
    if (requestCallback != null) {
      requestCallback.doWithRequest(clientHttpRequest);
    }
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

  @SuppressWarnings("rawtypes")
  @Getter(AccessLevel.PROTECTED)
  protected abstract static class AbstractPipedStreamSynchronizationBuilderImpl<T extends PipedStreamSynchronizer.PipedStreamSynchronizerBuilder>
      implements PipedStreamSynchronizer.PipedStreamSynchronizerBuilder<T> {
    private URI uri;
    private Authentication authentication;
    private RestOperations restOperations;

    @SuppressWarnings("unchecked")
    @Override
    public T authentication(Authentication authentication) {
      this.authentication = authentication;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T restOperations(RestOperations restOperations) {
      this.restOperations = restOperations;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T uri(URI uri) {
      this.uri = uri;
      return (T) this;
    }
  }

}
