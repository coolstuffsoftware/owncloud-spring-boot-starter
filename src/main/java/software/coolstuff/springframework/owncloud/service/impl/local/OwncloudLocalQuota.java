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

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileSystemUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;

/**
 * @author mufasa1976
 *
 */
@Data
@Getter(AccessLevel.NONE)
@ToString(exclude = "location")
@Builder
@Slf4j
class OwncloudLocalQuota implements OwncloudQuota, Cloneable {
  @Getter
  private final String username;
  private final Path location;
  private Long total;
  @Getter
  @Setter
  private long used = 0;

  static class OwncloudLocalQuotaBuilder {
    private long used = 0;
  }

  @Override
  public long getTotal() {
    if (total == null) {
      return getFreeSpaceOfLocation();
    }
    if (used > total) {
      return used;
    }
    return total;
  }

  private long getFreeSpaceOfLocation() {
    try {
      return FileSystemUtils.freeSpaceKb(location.toAbsolutePath().normalize().toString()) * 1024;
    } catch (IOException e) {
      String logMessage = "Error while getting the free Space of the Path " + location.toAbsolutePath().normalize().toString();
      log.error(logMessage);
      throw new OwncloudLocalResourceException(logMessage, e);
    }
  }

  public synchronized void increaseUsed(long size) {
    used += size;
  }

  public synchronized void reduceUsed(long size) {
    used -= size;
  }

  @Override
  public long getFree() {
    long free = getTotal() - used;
    return free > 0 ? free : 0;
  }

  @Override
  public float getRelative() {
    return Long.valueOf(used).floatValue() * 100f / Long.valueOf(getTotal()).floatValue();
  }

}
