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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

/**
 * advanced Properties for REST Implementation of the Owncloud Services
 *
 * @author mufasa1976
 */
@Validated
@Getter
@Setter
@ConfigurationProperties("owncloud")
public class OwncloudRestProperties extends OwncloudProperties {

  /**
   * advanced Properties for the REST Resource Service Implementation
   *
   * @author mufasa1976
   */
  @Validated
  @Getter
  @Setter
  public static class ResourceServiceProperties extends OwncloudProperties.ResourceServiceProperties {

    /**
     * Google-Cache Properties for the Sardine Library Cache
     *
     * @author mufasa1976
     */
    @Validated
    @Getter
    @Setter
    public static class CacheProperties {
      /** allowed update Concurrency */
      @Min(1)
      private Integer concurrencyLevel;
      /** When will the Entry be removed from the Cache after last Access */
      @Min(0)
      private Long expireAfterAccess;
      /** Time Unit for {@link #expireAfterAccess}. Default: <code>SECONDS</code> */
      private TimeUnit expireAfterAccessTimeUnit = TimeUnit.SECONDS;
      /** When will the Entry be removed from the Cache after last Write */
      @Min(0)
      private Long expireAfterWrite;
      /** Time Unit for {@link #expireAfterWrite}. Default: <code>SECONDS</code> */
      private TimeUnit expireAfterWriteTimeUnit = TimeUnit.SECONDS;
      /** Initial Capacity for the Cache */
      @Min(0)
      private Integer initialCapacity;
      /** maximum Entries for the Cache */
      @Min(0)
      private Long maximumSize;
      /** maximum Weight for the Cache */
      @Min(0)
      private Long maximumWeight;
      /** When will the Entry be refreshed after the last Write */
      @Min(1)
      private Long refreshAfterWrite;
      /** Time Unit for {@link #refreshAfterWrite}. Default: <code>SECONDS</code> */
      private TimeUnit refreshAfterWriteTimeUnit = TimeUnit.SECONDS;
    }

    /** Google-Cache Properties for the Sardine Library Cache */
    private CacheProperties cache = new CacheProperties();
  }

  /** advanced Properties for the REST Resource Service Implementation */
  private ResourceServiceProperties resourceService = new ResourceServiceProperties();

}
