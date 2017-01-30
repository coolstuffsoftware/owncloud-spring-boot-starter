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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
final class OwncloudRestUtils {

  public static List<String> convertGroups(Ocs.Groups ocsGroups) {
    List<String> groups = new ArrayList<>();
    if (isGroupsNotNull(ocsGroups)) {
      for (Ocs.Groups.Data.Group group : ocsGroups.getData().getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  private static boolean isGroupsNotNull(Ocs.Groups ocsGroups) {
    return ocsGroups != null && ocsGroups.getData() != null && ocsGroups.getData().getGroups() != null;
  }

  public static URL checkAndConvertLocation(String location) throws MalformedURLException {
    Validate.notBlank(location);
    log.debug("Check if the Location {} is a valid URL", location);
    URL url = new URL(location);
    log.debug("Check if the Location {} either start with http or https", location);
    if (isNotValidProtocol(url)) {
      final String exceptionMessage = "Invalid Protocol " + url.getProtocol() + ". Only http or https are allowed";
      log.error(exceptionMessage);
      throw new IllegalArgumentException(exceptionMessage);
    }
    return url;
  }

  private static boolean isNotValidProtocol(URL url) {
    return !StringUtils.equals(url.getProtocol(), "http") && !StringUtils.equals(url.getProtocol(), "https");
  }

  public static String appendDefaultPath(URL locationURL, String defaultPath) {
    log.debug("Extract the Root-URI from URL {}", locationURL);
    if (StringUtils.isBlank(locationURL.getPath()) || "/".equals(locationURL.getPath())) {
      return URI.create(locationURL.toString() + defaultPath).toString();
    }
    return locationURL.toString();
  }
}
