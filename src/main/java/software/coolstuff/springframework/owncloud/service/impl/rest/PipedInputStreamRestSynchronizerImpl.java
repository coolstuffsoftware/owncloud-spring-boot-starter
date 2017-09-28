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

import lombok.Builder;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudRestResourceException;

import java.io.*;
import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;

class PipedInputStreamRestSynchronizerImpl extends AbstractPipedStreamRestSynchronizerImpl implements PipedInputStreamRestSynchronizer {

  private final SynchronizedPipedInputStream pipedInputStream = new SynchronizedPipedInputStream();

  private PipedInputStreamRestSynchronizerImpl(
      final Authentication authentication,
      final URI uri,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    super(
        authentication,
        uri,
        owncloudRestProperties,
        restOperations,
        uriResolver);
  }

  @Builder(builderClassName = "PipedInputStreamRestSynchronizerBuilder")
  private static PipedInputStreamRestSynchronizer build(
      final Authentication authentication,
      final URI uri,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver) {
    return new PipedInputStreamRestSynchronizerImpl(
        authentication,
        uri,
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
                                                                      .runtimeExceptionHandler(pipedInputStream::setRuntimeException)
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

    private Optional<RuntimeException> runtimeException = Optional.empty();

    public void setRuntimeException(RuntimeException runtimeException) {
      this.runtimeException = Optional.ofNullable(runtimeException);
    }

    @Override
    public void close() throws IOException {
      super.close();
      runtimeException.ifPresent(this::handleRuntimeException);
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
