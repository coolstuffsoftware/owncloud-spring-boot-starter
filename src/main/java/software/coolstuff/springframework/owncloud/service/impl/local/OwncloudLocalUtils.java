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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
final class OwncloudLocalUtils {

  public static void validateUserNotNull(OwncloudLocalUserData.User user, String username) {
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
  }

  public static void checkPrivilegesOnDirectory(Path location) {
    log.debug("Check the Location {}", location);
    Validate.isTrue(Files.exists(location));
    Validate.isTrue(Files.isDirectory(location));
    Validate.isTrue(Files.isReadable(location)); // List Files within Directory
    Validate.isTrue(Files.isExecutable(location)); // can change into Directory
    Validate.isTrue(Files.isWritable(location)); // can create or delete Files within Directory
  }

}
