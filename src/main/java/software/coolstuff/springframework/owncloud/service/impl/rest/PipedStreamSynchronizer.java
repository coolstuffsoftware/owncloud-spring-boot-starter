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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.function.BiFunction;

import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestOperations;

import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;

/**
 * @author mufasa1976
 */
interface PipedStreamSynchronizer {

  void startAndWaitForConnectedPipe();

  @SuppressWarnings("rawtypes")
  interface PipedStreamSynchronizerBuilder<T extends PipedStreamSynchronizerBuilder> {

    T authentication(Authentication authentication);

    T owncloudFileResource(OwncloudFileResource owncloudFileResource);

    T owncloudRestProperties(OwncloudRestProperties owncloudRestProperties);

    T restOperations(RestOperations restOperations);

    T uriResolver(BiFunction<URI, String, URI> uriResolver);

    PipedStreamSynchronizer build();

  }

  interface PipedInputStreamSynchronizerBuilder extends PipedStreamSynchronizerBuilder<PipedInputStreamSynchronizerBuilder> {
    PipedInputStreamSynchronizerBuilder pipedInputStream(PipedInputStream pipedInputStream);
  }

  interface PipedOutputStreamSynchronizerBuilder extends PipedStreamSynchronizerBuilder<PipedOutputStreamSynchronizerBuilder> {
    PipedOutputStreamSynchronizerBuilder pipedOutputStream(PipedOutputStream pipedOutputStream);
  }

}
