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
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

/**
 * advanced Properties for local Implementation of the Owncloud Services
 *
 * @author mufasa1976
 * @since 1.0.0
 */
@Validated
@Getter
@Setter
@ConfigurationProperties("owncloud")
public class OwncloudLocalProperties extends OwncloudProperties {

  /**
   * advanced Properties for the local Resource Service Implementation
   *
   * @author mufasa1976
   * @since 1.2.0
   */
  @Validated
  @Getter
  @Setter
  public static class ResourceServiceProperties extends OwncloudProperties.ResourceServiceProperties {

    /**
     * Message Digest Algorithms for the Checksum Service.
     *
     * @author mufasa1976
     * @since 1.2.0
     */
    @RequiredArgsConstructor
    public static enum MessageDigestAlgorithm {
      /**
       * MD5
       * @since 1.2.0
       */
      MD5("MD5");

      private final String algorithm;

      /**
       * The Message Digest
       * @since 1.2.0
       */
      public MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(this.algorithm);
      }
    }

    /**
     * Message Digest Algorithm used by the Checksum Service
     * @since 1.2.0
     */
    @NotNull
    private MessageDigestAlgorithm messageDigestAlgorithm = MessageDigestAlgorithm.MD5;

    /**
     * Base Location for the WebDAV Resources
     * @since 1.2.0
     */
    private Path location;

    /**
     * File Name Prefix for the temporarily generated WebDAV Resource
     * @since 1.2.0
     */
    @NotNull
    private String pipedStreamTemporaryFilePrefix = "owncloud";
  }

  /**
   * advanced Properties for the local Resource Service Implementation
   * @since 1.2.0
   */
  private ResourceServiceProperties resourceService = new ResourceServiceProperties();
}
