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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceChecksumServiceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudLocalResourceException;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalProperties.ResourceServiceProperties;

@ActiveProfiles("LOCAL-RESOURCE-SERVICE")
@TestExecutionListeners({
    OwncloudLocalResourceServiceEmptyDirectoryTestExecutionListener.class
})
public class OwncloudLocalResourceServiceTest extends AbstractOwncloudResourceServiceTest {

  private final static String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  @Autowired
  private OwncloudLocalProperties properties;

  @MockBean
  private OwncloudLocalResourceChecksumService checksumService;

  @Autowired
  private OwncloudResourceService resourceService;

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
        Files.createDirectories(resourcePath.getParent());
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

  private void createFile(Path resourcePath, OwncloudTestFileResourceImpl owncloudFileResource) throws IOException {
    try (Writer writer = Files.newBufferedWriter(resourcePath)) {
      IOUtils.write(owncloudFileResource.getTestFileContent(), writer);
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
  protected void prepare_findFile_OK(URI searchPath, OwncloudTestFileResourceImpl expectedResource) throws Exception {
    createResource(expectedResource);
    modifyResourceInformationBasedOnPathInformation(expectedResource);
  }

  @Override
  protected void prepare_findRootDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    createResource(expectedResource);
    modifyResourceInformationBasedOnPathInformation(expectedResource);
  }

  @Override
  protected void prepare_getInputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    createResource(owncloudFileResource);
  };

  @Override
  protected void check_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(owncloudFileResource.getHref().getPath()));
    assertThat(resourcePath).exists();
    try (InputStream input = Files.newInputStream(resourcePath)) {
      String actual = new String(IOUtils.toByteArray(input));
      assertThat(actual).isEqualTo(owncloudFileResource.getTestFileContent());
    }
  }

  @Override
  protected void prepare_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(owncloudFileResource.getHref().getPath()));
    Mockito
        .doThrow(OwncloudLocalResourceException.class)
        .when(checksumService).recalculateChecksum(resourcePath);
  };

  @Override
  protected void prepare_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    OwncloudTestFileResourceImpl resource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(OwncloudTestResourceImpl.builder()
            .href(href)
            .mediaType(mediaType)
            .build())
        .testFileContent(testFileContent)
        .build();
    createResource(resource);
  }

  @Override
  protected void check_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(href.getPath()));
    assertThat(resourcePath).exists();
    assertThat(resourcePath).hasContent(testFileContent);
  }

  @Override
  protected void prepare_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
    OwncloudTestResourceImpl resource = OwncloudTestResourceImpl.builder()
        .href(href)
        .mediaType(OwncloudUtils.getDirectoryMediaType())
        .build();
    createResource(resource);
  }

  @Override
  protected void check_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(href.getPath()));
    assertThat(resourcePath).exists();
    assertThat(resourcePath).isDirectory();
  }

  @Override
  protected void prepare_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    OwncloudTestFileResourceImpl resource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(href)
                .mediaType(mediaType)
                .build())
        .testFileContent("Here we have a very different Content that should be overwritten by the Test-Content")
        .build();
    createResource(resource);
  }

  @Override
  protected void check_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(href.getPath()));
    assertThat(resourcePath).exists();
    assertThat(resourcePath).isRegularFile();
    assertThat(resourcePath).hasContent(testFileContent);
  }

  @Override
  protected void prepare_deleteFile_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    createResource(owncloudFileResource);
    Path resourcePath = resolveRelativePath(Paths.get(owncloudFileResource.getHref().getPath()));
    assertThat(resourcePath).exists();
  }

  @Override
  protected void check_deleteFile_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    Path resourcePath = resolveRelativePath(Paths.get(owncloudFileResource.getHref().getPath()));
    assertThat(resourcePath).doesNotExist();
  }

  @Override
  protected void prepare_deleteFile_NOK_OtherError(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
    createResource(owncloudFileResource);
    Path resourcePath = resolveRelativePath(Paths.get(owncloudFileResource.getHref().getPath()));
    Mockito
        .doThrow(OwncloudLocalResourceChecksumServiceException.class)
        .when(checksumService).recalculateChecksum(resourcePath);
  }

  @Override
  protected void prepare_deleteDirectory_OK(OwncloudTestResourceImpl owncloudResource) throws Exception {
    Path path = resolveRelativePath(Paths.get(owncloudResource.getHref().getPath()));
    Files.createDirectories(path);
    Files.createFile(path.resolve("someFile.txt"));
    Files.createFile(path.resolve("someOtherFile.txt"));
    Path subDirectory = Files.createDirectory(path.resolve("subDirectory"));
    Files.createFile(subDirectory.resolve("someFileInSubdirectory.txt"));
    Files.createFile(subDirectory.resolve("someOtherFileInSubdirectory.txt"));
  }

  @Override
  protected void check_deleteDirectory_OK(OwncloudTestResourceImpl owncloudResource) throws Exception {
    Path path = resolveRelativePath(Paths.get(owncloudResource.getHref().getPath()));
    assertThat(path).doesNotExist();
  }

  @Override
  protected void prepare_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    Path path = resolveRelativePath(Paths.get(expectedResource.getHref().getPath()));
    Mockito
        .when(checksumService.getChecksum(path))
        .thenReturn(Optional.ofNullable(expectedResource.getBackendETag()));
  }

  @Override
  protected void check_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
    Path path = resolveRelativePath(Paths.get(expectedResource.getHref().getPath()));
    assertThat(path).exists();
    assertThat(path).isDirectory();
  }

  @Override
  protected void prepare_createDirectory_NOK_AlreadyExistsAsFile(URI uri) throws Exception {
    Path path = resolveRelativePath(Paths.get(uri.getPath()));
    Files.createDirectories(path.getParent());
    Files.createFile(path);
  }

  @Override
  protected void prepare_createDirectory_OK_AlreadyExists(OwncloudTestResourceImpl expected) throws Exception {
    createResource(expected);
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_createBigFile_SecondFileFails_ModifyUserQuota_SecondFileWillBeWritten() throws Exception {
    MediaType mediaType = MediaType.TEXT_PLAIN;

    // check the quota (must be 1024)
    OwncloudQuota quota = resourceService.getQuota();
    assertThat(quota.getTotal()).isEqualTo(1024);
    assertThat(quota.getUsed()).isEqualTo(0);

    // first create a File with a Length of 768 Bytes
    URI uriBigFile = URI.create("/bigFile.txt");
    String bigFileContent = RandomStringUtils.random(768, RANDOM_CHARS.toCharArray());
    try (OutputStream output = resourceService.getOutputStream(uriBigFile, mediaType)) {
      IOUtils.write(bigFileContent, output, Charset.forName("utf8"));
    } finally {
      Path pathBigFile = resolveRelativePath(Paths.get(uriBigFile.getPath()));
      assertThat(pathBigFile).exists();
      assertThat(pathBigFile).isRegularFile();
      assertThat(pathBigFile).hasContent(bigFileContent);
    }

    quota = resourceService.getQuota();
    assertThat(quota.getUsed()).isEqualTo(768);

    // second try to create a File with a Length of 512 Bytes
    //    URI uriNotSoBigFile = URI.create("/notSoBigFile.txt");
    //    String notSoBigFileContent = RandomStringUtils.random(512, RANDOM_CHARS.toCharArray());
    //    OwncloudQuotaExceededException quotaExceeded = null;
    //    try (OutputStream output = resourceService.getOutputStream(uriNotSoBigFile, mediaType)) {
    //      IOUtils.write(notSoBigFileContent, output, Charset.forName("utf8"));
    //    } catch (OwncloudQuotaExceededException e) {
    //      quotaExceeded = e;
    //    } finally {
    //      Path pathNotSoBigFile = resolveRelativePath(Paths.get(uriNotSoBigFile.getPath()));
    //      assertThat(pathNotSoBigFile).doesNotExist();
    //    }
    //    assertThat(quotaExceeded).isNotNull();
  }
}
