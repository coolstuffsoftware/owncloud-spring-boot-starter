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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties.RefreshThreadProperties;

/**
 * @author mufasa1976
 */
@Slf4j
class OwncloudLocalResourceChecksumServiceRefreshingImpl extends AbstractOwncloudLocalResourceChecksumServiceImpl {

  private InitializingFileVisitor fileVisitor;
  private Thread refreshThread;

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    fileVisitor = InitializingFileVisitor.builder()
        .checksums(getChecksums())
        .fileDigest(this::createFileChecksum)
        .directoryDigest(this::createDirectoryChecksum)
        .build();
    ResourceServiceProperties resourceProperties = getResourceProperties();
    RefreshThreadProperties refreshThreadProperties = resourceProperties.getRefreshThread();
    refreshThread = new Thread(this::refresh);
    refreshThread.setName(refreshThreadProperties.getName());
    refreshThread.setPriority(refreshThreadProperties.getPriority());
    refreshThread.start();
  }

  private void refresh() {
    try {
      log.info("Refresh Thread {} has been started", Thread.currentThread().getName());
      ResourceServiceProperties resourceProperties = getResourceProperties();
      Path rootDirectory = resourceProperties.getLocation();
      RefreshThreadProperties refreshProperties = resourceProperties.getRefreshThread();
      TimeUnit refreshIntervalTimeUnit = refreshProperties.getRefreshIntervalTimeUnit();
      long sleepDuration = refreshIntervalTimeUnit.toMillis(refreshProperties.getRefreshInterval());

      for (;;) {
        try {
          fileVisitor.clearChecksums();
          fileVisitor.setStopVisitor(false);
          Files.walkFileTree(rootDirectory, fileVisitor);
          Thread.sleep(sleepDuration);
        } catch (IOException e) {
          log.error(String.format("Error while calculating the Checksums of Files under %s", rootDirectory), e);
        }
      }
    } catch (InterruptedException e) {
      log.info("Refresh Thread ({}) has been stopped", Thread.currentThread().getName());
    }
  }

  @PreDestroy
  public void destroy() throws Exception {
    if (refreshThread != null && refreshThread.isAlive()) {
      log.debug("Stopping Refresh Thread ({})", refreshThread.getName());
      fileVisitor.setStopVisitor(true);
      refreshThread.interrupt();
      refreshThread.join();
    }
  }

  @Override
  public ChecksumServiceStrategy getStrategy() {
    return ChecksumServiceStrategy.REFRESH;
  }

}
