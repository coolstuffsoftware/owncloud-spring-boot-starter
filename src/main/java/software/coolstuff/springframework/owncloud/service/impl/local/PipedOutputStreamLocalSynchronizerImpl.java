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
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.io.OutputStream;
import java.util.Optional;

import org.springframework.security.core.Authentication;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties.ResourceServiceProperties;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class PipedOutputStreamLocalSynchronizerImpl implements PipedOutputStreamLocalSynchronizer {

  private final Authentication authentication;
  private final OwncloudFileResource owncloudFileResource;
  private final OwncloudProperties owncloudProperties;

  @Builder
  private static PipedOutputStreamLocalSynchronizer build(
      final Authentication authentication,
      final OwncloudFileResource owncloudFileResource,
      final OwncloudProperties owncloudProperties) {
    return new PipedOutputStreamLocalSynchronizerImpl(authentication, owncloudFileResource, owncloudProperties);
  }

  @Override
  public OutputStream getOutputStream() {
    throw new RuntimeException("not implemented now");
  }

  private final int getBufferSize() {
    return Optional.ofNullable(owncloudProperties)
        .map(this::extractBufferSize)
        .orElse(OwncloudProperties.DEFAULT_BUFFER_SIZE);
  }

  private int extractBufferSize(OwncloudProperties properties) {
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    return resourceProperties.getPipedStreamBufferSize();
  }

}
