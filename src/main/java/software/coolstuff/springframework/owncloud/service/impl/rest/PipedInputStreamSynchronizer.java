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
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.function.BiFunction;

import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;

/**
 * @author mufasa1976
 */
class PipedInputStreamSynchronizer extends AbstractPipedStreamSynchronizerImpl {

  private final PipedInputStream pipedInputStream;

  private PipedInputStreamSynchronizer(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudRestProperties owncloudRestProperties,
      final RestOperations restOperations,
      final BiFunction<URI, String, URI> uriResolver,
      final PipedInputStream pipedInputStream) {
    super(authentication, owncloudFileResource, owncloudRestProperties, restOperations, uriResolver);
    this.pipedInputStream = pipedInputStream;
  }

  public static PipedStreamSynchronizer.PipedInputStreamSynchronizerBuilder builder() {
    return new PipedInputStreamSynchronizerBuilderImpl();
  }

  @Override
  protected HttpMethod getHttpMethod() {
    return HttpMethod.GET;
  }

  @Override
  public void createPipedStream() {
    try (OutputStream output = new PipedOutputStream(pipedInputStream)) {
      setPipeReady();
      execute(null, response -> copy(response.getBody(), output));
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 5448658359993578985L;
      };
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class PipedInputStreamSynchronizerBuilderImpl extends AbstractPipedStreamSynchronizationBuilderImpl<PipedStreamSynchronizer.PipedInputStreamSynchronizerBuilder>
      implements PipedStreamSynchronizer.PipedInputStreamSynchronizerBuilder {
    private PipedInputStream pipedInputStream;

    @Override
    public PipedStreamSynchronizer.PipedInputStreamSynchronizerBuilder pipedInputStream(PipedInputStream pipedInputStream) {
      this.pipedInputStream = pipedInputStream;
      return this;
    }

    @Override
    public PipedStreamSynchronizer build() {
      return new PipedInputStreamSynchronizer(
          getAuthentication(),
          getOwncloudFileResource(),
          getOwncloudRestProperties(),
          getRestOperations(),
          getUriResolver(),
          pipedInputStream);
    }

  }
}
