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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;

@Slf4j
class PipedOutputStreamRestSynchronizerImpl extends AbstractPipedStreamRestSynchronizerImpl implements PipedOutputStreamRestSynchronizer {

  private final Optional<MediaType> mediaType;
  private final SynchronizedPipedOutputStream pipedOutputStream;

  private PipedOutputStreamRestSynchronizerImpl(
      final Authentication authentication,
      final URI uri,
      final MediaType mediaType,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    super(authentication, uri, owncloudRestProperties, restOperations, uriResolver);
    this.pipedOutputStream = new SynchronizedPipedOutputStream();
    this.mediaType = Optional.ofNullable(mediaType);
  }

  @Builder
  private static PipedOutputStreamRestSynchronizer build(
      final Authentication authentication,
      final URI uri,
      final MediaType mediaType,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    return new PipedOutputStreamRestSynchronizerImpl(
        authentication,
        uri,
        mediaType,
        owncloudRestProperties,
        restOperations,
        uriResolver);
  }

  @Override
  protected HttpMethod getHttpMethod() {
    return HttpMethod.PUT;
  }

  @Override
  protected void createPipedStream() {
    try (InputStream input = new PipedInputStream(pipedOutputStream)) {
      setPipeReady();
      ExecutionEnvironment executionEnvironment = ExecutionEnvironment.builder()
          .requestCallback(clientHttpRequest -> setMediaTypeAndCopy(input, clientHttpRequest))
          .runtimeExceptionHandler(pipedOutputStream::setRuntimeException)
          .afterExecutionCallback(this::waitForPipeReady)
          .build();
      execute(executionEnvironment);
    } catch (IOException e) {
      throw new OwncloudRestResourceException(e);
    }
  }

  private void setMediaTypeAndCopy(InputStream input, ClientHttpRequest clientHttpRequest) throws IOException {
    addContentTypeHeader(clientHttpRequest);
    copy(input, clientHttpRequest.getBody());
  }

  private void addContentTypeHeader(ClientHttpRequest clientHttpRequest) {
    mediaType.ifPresent(mediaType -> {
      log.debug("Set the ContentType Header to {}", mediaType.toString());
      HttpHeaders headers = clientHttpRequest.getHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, mediaType.toString());
    });
  }

  @Override
  public OutputStream getOutputStream() {
    startThreadAndWaitForConnectedPipe();
    return pipedOutputStream;
  }

  private class SynchronizedPipedOutputStream extends PipedOutputStream {

    private Optional<RuntimeException> runtimeException = Optional.empty();

    public void setRuntimeException(RuntimeException runtimeException) {
      this.runtimeException = Optional.ofNullable(runtimeException);
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      } finally {
        setPipeReady();
        runtimeException.ifPresent(this::handleRuntimeException);
      }
    }

    private void handleRuntimeException(RuntimeException exception) {
      if (exception instanceof RestClientException) {
        handleRestClientException((RestClientException) exception);
      }
      throw exception;
    }

    private void handleRestClientException(RestClientException exception) {
      RestClientExceptionHandlerEnvironment exceptionHandlerEnvironment = RestClientExceptionHandlerEnvironment.builder()
          .restClientException(exception)
          .requestURI(getUri())
          .username(getUsername())
          .build();
      OwncloudRestUtils.handleRestClientException(exceptionHandlerEnvironment);
    }
  }
}
