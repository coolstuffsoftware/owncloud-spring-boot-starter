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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;

/**
 * @author mufasa1976
 */
class PipedInputStreamSynchronizerImpl extends AbstractPipedStreamSynchronizerImpl implements PipedInputStreamSynchronizer {

  private final SynchronizedPipedInputStream pipedInputStream = new SynchronizedPipedInputStream();

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
    registerRestClientExceptionHandler(HttpStatusCodeException.class, restClientException -> handleHttpStatusCodeException((HttpStatusCodeException) restClientException));
  }

  private void handleHttpStatusCodeException(HttpStatusCodeException httpStatusCodeException) {
    if (httpStatusCodeException.getStatusCode() == HttpStatus.NOT_FOUND) {
      throw new OwncloudResourceNotFoundException(getResolvedURI());
    }
  }

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

  @Override
  protected HttpMethod getHttpMethod() {
    return HttpMethod.GET;
  }

  @Override
  protected void createPipedStream() {
    try (OutputStream output = new PipedOutputStream(pipedInputStream)) {
      setPipeReady();
      execute(null, response -> copy(response.getBody(), output), pipedInputStream::setRestClientException);
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 5448658359993578985L;
      };
    }
  }

  @Override
  public InputStream getInputStream() {
    startThreadAndWaitForConnectedPipe();
    return pipedInputStream;
  }

  @RequiredArgsConstructor
  private class SynchronizedPipedInputStream extends PipedInputStream {

    private Optional<RestClientException> restClientException = Optional.empty();

    public void setRestClientException(RestClientException restClientException) {
      this.restClientException = Optional.of(restClientException);
    }

    @Override
    public void close() throws IOException {
      super.close();
      restClientException.ifPresent(restClientException -> handleRestClientException(restClientException));
    }

  }
}
