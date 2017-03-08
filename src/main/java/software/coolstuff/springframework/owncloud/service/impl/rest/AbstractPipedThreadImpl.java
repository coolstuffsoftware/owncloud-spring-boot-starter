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
import java.util.concurrent.CyclicBarrier;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestOperations;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * @author mufasa1976
 */
abstract class AbstractPipedThreadImpl implements PipedStreamThread {

  @FunctionalInterface
  static interface VoidResponseExtractor {
    void extractData(ClientHttpResponse clientHttpResponse) throws IOException;
  }

  private final URI uri;
  @Getter(AccessLevel.PROTECTED)
  private final Authentication authentication;
  private final RestOperations restOperations;

  private final CyclicBarrier pipeSync = new CyclicBarrier(2);

  protected AbstractPipedThreadImpl(
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
    thread.start();
    try {
      pipeSync.await();
    } catch (Exception e) {}
  }

  protected void setThreadName(HttpMethod httpMethod) {
    Thread.currentThread().setName(httpMethod + " " + uri);
  }

  protected void execute(HttpMethod httpMethod, RequestCallback requestCallback, VoidResponseExtractor responseExtractor) {
    if (responseExtractor == null) {
      restOperations.execute(uri, httpMethod, requestCallback, null);
    } else {
      restOperations.execute(uri, httpMethod, requestCallback, response -> {
        responseExtractor.extractData(response);
        return null;
      });
    }
  }

  protected abstract void createPipedStream();

}
