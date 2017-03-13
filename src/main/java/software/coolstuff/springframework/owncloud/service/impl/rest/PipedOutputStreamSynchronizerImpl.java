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

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;

@Slf4j
class PipedOutputStreamSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl implements PipedOutputStreamSynchronizer {

  private final SynchronizedPipedOutputStream pipedOutputStream;

  private PipedOutputStreamSynchronizerImpl(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    super(authentication, owncloudFileResource, owncloudRestProperties, restOperations, uriResolver);
    this.pipedOutputStream = new SynchronizedPipedOutputStream();
  }

  @Builder
  private static PipedOutputStreamSynchronizer build(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    return new PipedOutputStreamSynchronizerImpl(
        authentication,
        owncloudFileResource,
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
          .restClientExceptionHandler(pipedOutputStream::setRestClientException)
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

  @Override
  public OutputStream getOutputStream() {
    startThreadAndWaitForConnectedPipe();
    return pipedOutputStream;
  }

  private class SynchronizedPipedOutputStream extends PipedOutputStream {

    private Optional<RestClientException> restClientException = Optional.empty();

    public void setRestClientException(RestClientException restClientException) {
      this.restClientException = Optional.ofNullable(restClientException);
    }

    @Override
    public void close() throws IOException {
      super.close();
      setPipeReady();
      restClientException.ifPresent(restClientException -> {
        RestClientExceptionHandlerEnvironment exceptionHandlerEnvironment = RestClientExceptionHandlerEnvironment.builder()
            .restClientException(restClientException)
            .requestURI(getUnresolvedUri())
            .username(getUsername())
            .build();
        OwncloudRestUtils.handleRestClientException(exceptionHandlerEnvironment);
      });
    }
  }
}
