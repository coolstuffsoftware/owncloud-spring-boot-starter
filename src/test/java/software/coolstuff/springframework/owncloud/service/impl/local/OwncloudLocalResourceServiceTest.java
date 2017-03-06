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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@ActiveProfiles("LOCAL-RESOURCE-SERVICE")
@TestExecutionListeners({
    OwncloudLocalResourceServiceEmptyDirectoryTestExecutionListener.class
})
public class OwncloudLocalResourceServiceTest extends AbstractOwncloudResourceServiceTest {

  private final static String FILE_CONTENT = "This is a Test";

  @Autowired
  private OwncloudLocalProperties properties;

  @MockBean
  private OwncloudLocalResourceChecksumService checksumService;

  @Override
  protected Class<? extends OwncloudResourceService> getImplementationClass() {
    return OwncloudLocalResourceServiceImpl.class;
  }

  @Override
  protected OwncloudResource prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(OwncloudResource expected) throws Exception {
    return OwncloudLocalResourceImpl.builder()
        .eTag(expected.getETag())
        .href(expected.getHref())
        .lastModifiedAt(expected.getLastModifiedAt())
        .mediaType(expected.getMediaType())
        .name(expected.getName())
        .build();
  }

  @Override
  protected void prepare_listRoot_OK(List<OwncloudTestResourceImpl> expectedResources) throws Exception {
    expectedResources.stream().forEach(this::createResource);
    expectedResources.stream().forEach(this::modifyResourceInformationBasedOnPathInformation);
  }

  private void createResource(OwncloudTestResourceImpl owncloudResource) {
    Path resourcePath = resolveRelativePath(Paths.get(owncloudResource.getHref().getPath()));
    try {
      Mockito
          .when(checksumService.getChecksum(resourcePath))
          .thenReturn(Optional.ofNullable(owncloudResource.getBackendETag()));
      if (owncloudResource instanceof OwncloudTestFileResourceImpl) {
        createFile(resourcePath, (OwncloudTestFileResourceImpl) owncloudResource);
      } else {
        if (!Files.exists(resourcePath)) {
          Files.createDirectories(resourcePath);
        } else if (!Files.isDirectory(resourcePath)) {
          throw new IllegalStateException("Path " + resourcePath + " is not a Directory");
        }
      }
    } catch (IOException e) {
      e.printStackTrace(System.err);
      throw new IllegalStateException("Error while preparing Resource " + resourcePath + ". Cause: " + e.toString(), e);
    }
  }

  private Path resolveRelativePath(Path relativePath) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path basePath = resourceProperties.getLocation()
        .resolve(authentication.getName());
    if (relativePath.isAbsolute()) {
      String relativizedPath = StringUtils.substring(relativePath.toString(), File.pathSeparator.length());
      return basePath.resolve(relativizedPath);
    }
    return basePath.resolve(relativePath);
  }

  private void createFile(Path resourcePath, OwncloudTestFileResourceImpl owncloudResource) throws IOException {
    try (Writer writer = Files.newBufferedWriter(resourcePath)) {
      IOUtils.write(FILE_CONTENT, writer);
    }
  }

  private void modifyResourceInformationBasedOnPathInformation(OwncloudTestResourceImpl owncloudResource) {
    Path resourcePath = resolveRelativePath(Paths.get(owncloudResource.getHref().getPath()));
    try {
      owncloudResource.setLastModifiedAt(Date.from(Files.getLastModifiedTime(resourcePath).toInstant()));
    } catch (IOException e) {
      e.printStackTrace(System.err);
      throw new IllegalStateException("Error while getting last modified time of Resource " + resourcePath, e);
    }
  }

  @Override
  protected void prepare_list_OK(URI searchPath, List<OwncloudTestResourceImpl> expectedResources) throws Exception {
    expectedResources.stream().forEach(this::createResource);
    expectedResources.stream().forEach(this::modifyResourceInformationBasedOnPathInformation);
  }

  @Override
  protected void prepare_list_NOK_FileNoutFound(URI searchPath) throws Exception {}

}
