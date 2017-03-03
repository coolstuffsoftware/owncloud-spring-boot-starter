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

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import lombok.val;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

/**
 * @author mufasa1976
 */
class OwncloudLocalResourceChecksumServiceManualImpl extends AbstractOwncloudLocalResourceChecksumServiceImpl {

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    ResourceServiceProperties resourceProperties = getResourceProperties();
    Path rootDirectory = resourceProperties.getLocation();
    val fileVisitor = InitializingFileVisitor.builder()
        .checksums(getChecksums())
        .fileDigest(this::createFileChecksum)
        .directoryDigest(this::createDirectoryChecksum)
        .build();
    Files.walkFileTree(rootDirectory, fileVisitor);
  }

  @Override
  public OwncloudLocalResourceChecksumServiceStrategy getStrategy() {
    return OwncloudLocalResourceChecksumServiceStrategy.MANUAL;
  }

}
