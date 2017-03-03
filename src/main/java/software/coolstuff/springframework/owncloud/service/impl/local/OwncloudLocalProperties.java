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
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  static class ResourceServiceProperties extends OwncloudProperties.ResourceServiceProperties {

    @RequiredArgsConstructor
    static enum MessageDigestAlgorithm {

      MD5("MD5");

      private final String algorithm;

      public MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(this.algorithm);
      }

    }

    @Data
    private static class ThreadProperties {
      @NotNull
      private String name;

      @Min(Thread.MIN_PRIORITY)
      @Max(Thread.MAX_PRIORITY)
      private int priority = Thread.NORM_PRIORITY;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class FileWatcherThreadProperties extends ThreadProperties {

      public FileWatcherThreadProperties() {
        setName("Owncloud local Resources Checksum File Watcher");
      }

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    static class RefreshThreadProperties extends ThreadProperties {

      public RefreshThreadProperties() {
        setName("Owncloud local Resources Checksum Refresh");
      }

      @NotNull
      private int refreshInterval = 5;
      @NotNull
      private TimeUnit refreshIntervalTimeUnit = TimeUnit.MINUTES;
    }

    private Path location;
    private FileWatcherThreadProperties fileWatcherThread = new FileWatcherThreadProperties();
    private RefreshThreadProperties refreshThread = new RefreshThreadProperties();
    @NotNull
    private MessageDigestAlgorithm messageDigestAlgorithm = MessageDigestAlgorithm.MD5;
    @NotNull
    private OwncloudLocalResourceChecksumServiceStrategy checksumServiceStrategy = OwncloudLocalResourceChecksumServiceStrategy.FILE_WATCHER;
  }

  private ResourceServiceProperties resourceService = new ResourceServiceProperties();

}
