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

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;

/**
 * @author mufasa1976
 */
class PipedInputStreamThread extends AbstractPipedThreadImpl {

  private final PipedInputStream pipedInputStream;

  private PipedInputStreamThread(
      final URI uri,
      final Authentication authentication,
      final RestOperations restOperations,
      final PipedInputStream pipedInputStream) {
    super(uri, authentication, restOperations);
    this.pipedInputStream = pipedInputStream;
  }

  public static PipedStreamThread.PipedInputStreamThreadBuilder builder() {
    return new PipedInputStreamThreadBuilder();
  }

  @Override
  public void createPipedStream() {
    setThreadName(HttpMethod.GET);
    try (OutputStream output = new PipedOutputStream(pipedInputStream)) {
      setPipeReady();
      execute(HttpMethod.GET, this::handleRequest, response -> IOUtils.copy(response.getBody(), output));
    } catch (IOException e) {
      throw new OwncloudResourceException(e) {
        private static final long serialVersionUID = 5448658359993578985L;
      };
    }
  }

  private void handleRequest(ClientHttpRequest clientHttpRequest) throws IOException {
    OwncloudRestResourceServiceImpl.addMissingAuthorizationHeader(clientHttpRequest, getAuthentication());
    setKeepAlive(clientHttpRequest);
  }

  private void setKeepAlive(ClientHttpRequest clientHttpRequest) {
    HttpHeaders headers = clientHttpRequest.getHeaders();
    headers.add(HttpHeaders.CONNECTION, "keep-alive");
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class PipedInputStreamThreadBuilder implements PipedStreamThread.PipedInputStreamThreadBuilder {
    private URI uri;
    private Authentication authentication;
    private RestOperations restOperations;
    private PipedInputStream pipedInputStream;

    @Override
    public PipedStreamThread.PipedInputStreamThreadBuilder uri(URI uri) {
      this.uri = uri;
      return this;
    }

    @Override
    public PipedStreamThread.PipedInputStreamThreadBuilder authentication(Authentication authentication) {
      this.authentication = authentication;
      return this;
    }

    @Override
    public PipedStreamThread.PipedInputStreamThreadBuilder restOperations(RestOperations restOperations) {
      this.restOperations = restOperations;
      return this;
    }

    @Override
    public PipedStreamThread.PipedInputStreamThreadBuilder pipedInputStream(PipedInputStream pipedInputStream) {
      this.pipedInputStream = pipedInputStream;
      return this;
    }

    @Override
    public PipedStreamThread build() {
      return new PipedInputStreamThread(uri, authentication, restOperations, pipedInputStream);
    }

  }
}
