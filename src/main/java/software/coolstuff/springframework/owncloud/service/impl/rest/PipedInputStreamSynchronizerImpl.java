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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.Builder;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;

class PipedInputStreamSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl implements PipedInputStreamSynchronizer {

  private final SynchronizedPipedInputStream pipedInputStream = new SynchronizedPipedInputStream();

  @Builder
  private static PipedInputStreamSynchronizer build(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    return new PipedInputStreamSynchronizerImpl(
        authentication,
        owncloudFileResource,
        owncloudRestProperties,
        restOperations,
        uriResolver);
  }

  private PipedInputStreamSynchronizerImpl(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    super(
        authentication,
        owncloudFileResource,
        owncloudRestProperties,
        restOperations,
        uriResolver);
  }

  @Override
  protected HttpMethod getHttpMethod() {
    return HttpMethod.GET;
  }

  @Override
  protected void createPipedStream() {
    try (OutputStream output = new PipedOutputStream(pipedInputStream)) {
      setPipeReady();
      ExecutionEnvironment executionEnvironment = ExecutionEnvironment.builder()
          .responseExtractor(response -> copy(response.getBody(), output))
          .restClientExceptionHandler(pipedInputStream::setRestClientException)
          .build();
      execute(executionEnvironment);
    } catch (IOException e) {
      throw new OwncloudRestResourceException(e);
    }
  }

  @Override
  public InputStream getInputStream() {
    startThreadAndWaitForConnectedPipe();
    return pipedInputStream;
  }

  private class SynchronizedPipedInputStream extends PipedInputStream {

    private Optional<RestClientException> restClientException = Optional.empty();

    public void setRestClientException(RestClientException restClientException) {
      this.restClientException = Optional.ofNullable(restClientException);
    }

    @Override
    public void close() throws IOException {
      super.close();
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
