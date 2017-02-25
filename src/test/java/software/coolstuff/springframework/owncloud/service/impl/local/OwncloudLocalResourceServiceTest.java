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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@ActiveProfiles("LOCAL-RESOURCE-SERVICE")
@TestExecutionListeners({
    OwncloudLocalResourceServiceTestExecutionListener.class
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
  protected List<OwncloudResource> prepare_listRoot_OK() throws Exception {
    List<OwncloudResource> expectedOwncloudResources = Lists.newArrayList(
        createDirectory(Paths.get("."), "."),
        createFile(Paths.get("resource1.pdf"), MediaType.APPLICATION_PDF));
    return expectedOwncloudResources;
  }

  private OwncloudResource createDirectory(Path relativePath, String renameTo) throws IOException {
    Path resourcePath = resolveRelativePath(relativePath).normalize();
    Files.createDirectories(resourcePath);
    String uriPath = Optional.ofNullable(relativePath)
        .map(path -> path.normalize())
        .map(path -> path.toString())
        .orElse("/");
    if (StringUtils.isBlank(uriPath)) {
      uriPath = "/";
    }
    URI href = URI.create(
        UriComponentsBuilder
            .fromPath(uriPath)
            .toUriString());
    String directoryName = Optional.ofNullable(renameTo)
        .orElse(relativePath.getFileName().toString());
    String checksum = UUID.randomUUID().toString();
    Mockito
        .when(checksumService.getChecksum(resourcePath))
        .thenReturn(checksum);
    return OwncloudLocalResourceImpl.builder()
        .eTag(checksum)
        .href(href)
        .lastModifiedAt(new Date())
        .mediaType(OwncloudUtils.getDirectoryMediaType())
        .name(directoryName)
        .build();
  }

  private Path resolveRelativePath(Path relativePath) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ResourceServiceProperties resourceProperties = properties.getResourceService();
    Path resourcePath = resourceProperties.getLocation()
        .resolve(authentication.getName())
        .resolve(relativePath);
    return resourcePath;
  }

  private OwncloudResource createFile(Path relativePath, MediaType mediaType) throws IOException {
    Path resourcePath = resolveRelativePath(relativePath).normalize();
    try (Writer writer = new BufferedWriter(new FileWriter(resourcePath.toFile()))) {
      IOUtils.write(FILE_CONTENT, writer);
    }
    URI href = URI.create(
        UriComponentsBuilder
            .fromPath(relativePath.normalize().toString())
            .toUriString());
    String fileName = relativePath.getFileName().toString();
    String checksum = UUID.randomUUID().toString();
    Mockito
        .when(checksumService.getChecksum(resourcePath))
        .thenReturn(checksum);
    return OwncloudLocalFileResourceImpl.fileBuilder()
        .owncloudResource(OwncloudLocalResourceImpl.builder()
            .eTag(checksum)
            .href(href)
            .lastModifiedAt(new Date())
            .mediaType(mediaType)
            .name(fileName)
            .build())
        .contentLength(Long.valueOf(FILE_CONTENT.length()))
        .build();
  }

}
