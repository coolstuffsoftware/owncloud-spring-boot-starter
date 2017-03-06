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
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ConfigurationProperties("owncloud")
class OwncloudLocalProperties extends OwncloudProperties {

  @RequiredArgsConstructor
  static enum MessageDigestAlgorithm {

    MD5("MD5");

    private final String algorithm;

    public MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
      return MessageDigest.getInstance(this.algorithm);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  static class ResourceServiceProperties extends OwncloudProperties.ResourceServiceProperties {
    private Path location;
    @NotNull
    private MessageDigestAlgorithm messageDigestAlgorithm = MessageDigestAlgorithm.MD5;
  }

  private ResourceServiceProperties resourceService = new ResourceServiceProperties();
}
