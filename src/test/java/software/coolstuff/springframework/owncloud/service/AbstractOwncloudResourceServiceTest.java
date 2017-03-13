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
package software.coolstuff.springframework.owncloud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Lists;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceException;
import software.coolstuff.springframework.owncloud.exception.resource.OwncloudResourceNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        VelocityConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    MockitoTestExecutionListener.class,
    ResetMocksTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
})
@ComponentScan(excludeFilters = @Filter(IgnoreOnComponentScan.class))
@RestClientTest(OwncloudResourceService.class)
public abstract class AbstractOwncloudResourceServiceTest {

  private static final String TEST_FILE_CONTENT = "This is a Test";

  @Autowired
  private OwncloudResourceService resourceService;

  @Test
  public void test_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl() throws Exception {
    String eTag = UUID.randomUUID().toString();
    OwncloudResource expected = OwncloudTestResourceImpl.builder()
        .backendETag(eTag)
        .backendName("user")
        .eTag(eTag)
        .href(URI.create("https://owncloud.example.com/remote.php/dav/files/user/"))
        .lastModifiedAt(new Date())
        .mediaType(OwncloudUtils.getDirectoryMediaType())
        .name(".")
        .build();
    OwncloudResource actual = prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(expected);
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
  }

  protected abstract OwncloudResource prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(OwncloudResource expected) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_listRoot_OK() throws Exception {
    String eTag = UUID.randomUUID().toString();
    List<OwncloudTestResourceImpl> expectedResources = Lists.newArrayList(
        OwncloudTestResourceImpl.builder()
            .backendETag(UUID.randomUUID().toString())
            .backendName("user")
            .href(URI.create("/"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name(".")
            .build(),
        OwncloudTestFileResourceImpl.fileBuilder()
            .owncloudResource(OwncloudTestFileResourceImpl.builder()
                .backendETag(eTag)
                .backendName("resource1")
                .eTag(eTag)
                .href(URI.create("/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_OCTET_STREAM)
                .name("resource1")
                .build())
            .testFileContent(TEST_FILE_CONTENT)
            .build());
    prepare_listRoot_OK(expectedResources);
    List<OwncloudResource> resources = resourceService.listRoot();
    assertThat(resources).containsOnlyElementsOf(expectedResources);
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  @ToString
  public static class OwncloudTestResourceImpl implements OwncloudResource {
    private URI href;
    private String name;
    private String backendName;
    @Setter
    private Date lastModifiedAt;
    private MediaType mediaType;
    private String eTag;
    private String backendETag;
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class OwncloudTestFileResourceImpl extends OwncloudTestResourceImpl implements OwncloudFileResource {

    private String testFileContent;

    @Builder(builderMethodName = "fileBuilder")
    private OwncloudTestFileResourceImpl(OwncloudTestResourceImpl owncloudResource, String testFileContent) {
      super(
          owncloudResource.getHref(),
          owncloudResource.getName(),
          owncloudResource.getBackendName(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getMediaType(),
          owncloudResource.getETag(),
          owncloudResource.getBackendETag());
      this.testFileContent = testFileContent;
    }

    @Override
    public Long getContentLength() {
      return Long.valueOf(testFileContent.length());
    }
  }

  protected abstract void prepare_listRoot_OK(List<OwncloudTestResourceImpl> expectedResources) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_list_OK() throws Exception {
    URI searchPath = URI.create("/directory/directory/");
    String eTagSearchPath = UUID.randomUUID().toString();
    String eTagFileResource = UUID.randomUUID().toString();
    String eTagSuperPath = UUID.randomUUID().toString();
    List<OwncloudTestResourceImpl> expectedResources = Lists.newArrayList(
        OwncloudTestResourceImpl.builder()
            .backendETag(eTagSearchPath)
            .backendName("directory")
            .eTag(eTagSearchPath)
            .href(appendPath(searchPath, "/"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name(".")
            .build(),
        OwncloudTestFileResourceImpl.fileBuilder()
            .owncloudResource(OwncloudTestResourceImpl.builder()
                .backendETag(eTagFileResource)
                .backendName("resource1")
                .eTag(eTagFileResource)
                .href(appendPath(searchPath, "/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_OCTET_STREAM)
                .name("resource1")
                .build())
            .testFileContent(TEST_FILE_CONTENT)
            .build(),
        OwncloudTestResourceImpl.builder()
            .backendETag(eTagSuperPath)
            .backendName("directory")
            .eTag(eTagSuperPath)
            .href(appendPath(searchPath, "/../"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name("..")
            .build());
    prepare_list_OK(searchPath, expectedResources);
    List<OwncloudResource> resources = resourceService.list(searchPath);
    assertThat(resources).containsOnlyElementsOf(expectedResources);
  }

  protected abstract void prepare_list_OK(URI searchPath, List<OwncloudTestResourceImpl> expectedOwncloudResources) throws Exception;

  private URI appendPath(URI baseUri, String appendPath) {
    if (baseUri == null) {
      return Optional.ofNullable(appendPath)
          .map(path -> URI.create(path).normalize())
          .orElse(baseUri);
    }
    return URI.create(
        UriComponentsBuilder.fromUri(baseUri)
            .path(appendPath)
            .toUriString())
        .normalize();
  }

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_list_NOK_FileNotFound() throws Exception {
    URI searchPath = URI.create("/unknown");
    prepare_list_NOK_FileNotFound(searchPath);
    resourceService.list(searchPath);
    fail("Expected Exception " + OwncloudResourceNotFoundException.class.getName() + " has not been thrown");
  }

  protected abstract void prepare_list_NOK_FileNotFound(URI searchPath) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_findFile_OK() throws Exception {
    URI searchPath = URI.create("/file.txt");
    String eTag = UUID.randomUUID().toString();
    OwncloudTestFileResourceImpl expectedResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(OwncloudTestResourceImpl.builder()
            .backendETag(eTag)
            .backendName("file.txt")
            .eTag(eTag)
            .href(searchPath)
            .lastModifiedAt(new Date())
            .mediaType(MediaType.TEXT_PLAIN)
            .name("file.txt")
            .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_findFile_OK(searchPath, expectedResource);
    OwncloudResource resource = resourceService.find(searchPath);
    assertThat(resource).isNotNull();
    assertThat(resource).isEqualTo(expectedResource);
  }

  protected abstract void prepare_findFile_OK(URI searchPath, OwncloudTestFileResourceImpl expectedResource) throws Exception;

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_findFile_NotExists() throws Exception {
    URI searchPath = URI.create("/unknownFile.txt");
    prepare_findFile_NotExists(searchPath);
    resourceService.find(searchPath);
    fail("Expected Exception " + OwncloudResourceNotFoundException.class.getName() + " has not been thrown");
  }

  protected abstract void prepare_findFile_NotExists(URI searchPath) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_findRootDirectory_OK() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudTestResourceImpl expected = OwncloudTestResourceImpl.builder()
        .backendETag(UUID.randomUUID().toString())
        .backendName(authentication.getName())
        .eTag(null)
        .href(URI.create("/"))
        .lastModifiedAt(new Date())
        .mediaType(OwncloudUtils.getDirectoryMediaType())
        .name("/")
        .build();
    prepare_findRootDirectory_OK(expected);
    OwncloudResource actual = resourceService.find(null);
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
  }

  protected abstract void prepare_findRootDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_getInputStream_OK() throws Exception {
    URI href = URI.create("/directory/fileInDirectory.txt");
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(OwncloudTestResourceImpl.builder()
            .href(href)
            .mediaType(MediaType.TEXT_PLAIN)
            .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_getInputStream_OK(owncloudFileResource);
    try (InputStream input = resourceService.getInputStream(owncloudFileResource)) {
      byte[] actual = IOUtils.toByteArray(input);
      byte[] expected = owncloudFileResource.getTestFileContent().getBytes();
      assertThat(actual).isEqualTo(expected);
    }
    check_getInputStream_OK();
  }

  protected abstract void prepare_getInputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_getInputStream_OK() throws Exception;

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_getInputStream_NOK_FileNotFound() throws Exception {
    URI href = URI.create("/directory/notExists.txt");
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(href)
                .mediaType(MediaType.TEXT_PLAIN)
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_getInputStream_NOK_FileNotFound(owncloudFileResource);
    try (InputStream input = resourceService.getInputStream(owncloudFileResource)) {
      input.read(); // we have to read at least one byte
    }
    check_getInputStream_NOK_FileNotFound(owncloudFileResource);
  }

  protected abstract void prepare_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  protected void check_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) {
    fail("OwncloudResourceNotFoundException should be thrown");
  }

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_OK() throws Exception {
    URI href = URI.create("/createdFile.txt");
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(href)
                .mediaType(MediaType.TEXT_PLAIN)
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_getOutputStream_OK(owncloudFileResource);
    try (OutputStream output = resourceService.getOutputStream(owncloudFileResource)) {
      IOUtils.write(owncloudFileResource.getTestFileContent(), output, Charset.forName("utf8"));
    } finally {
      check_getOutputStream_OK(owncloudFileResource);
    }
  }

  protected abstract void prepare_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  @Test(expected = OwncloudResourceException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_NOK_Unauthorized() throws Exception {
    URI href = URI.create("/createdFile.txt");
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(href)
                .mediaType(MediaType.TEXT_PLAIN)
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_getOutputStream_NOK_Unauthorized(owncloudFileResource);
    try (OutputStream output = resourceService.getOutputStream(owncloudFileResource)) {
      output.write(1); // at least try to write 1 Byte
    } finally {
      check_getOutputStream_NOK_Unauthorized(owncloudFileResource);
    }
  }

  protected abstract void prepare_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_OK_CreateNewFile() throws Exception {
    URI href = URI.create("/createdFile.txt");
    MediaType mediaType = MediaType.TEXT_PLAIN;
    prepare_getOutputStream_OK_CreateNewFile(href, mediaType, TEST_FILE_CONTENT);
    try (OutputStream output = resourceService.getOutputStream(href, mediaType)) {
      IOUtils.write(TEST_FILE_CONTENT, output, Charset.forName("utf8"));
    }
    check_getOutputStream_OK_CreateNewFile(href, mediaType, TEST_FILE_CONTENT);
  }

  protected abstract void prepare_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception;

  protected abstract void check_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_OK() throws Exception {
    OwncloudTestResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(URI.create("/createdFile.txt"))
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_deleteFile_OK(owncloudFileResource);
    resourceService.delete(owncloudFileResource);
    check_deleteFile_OK(owncloudFileResource);
  }

  protected abstract void prepare_deleteFile_OK(OwncloudTestResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_deleteFile_OK(OwncloudTestResourceImpl owncloudFileResource) throws Exception;

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_NOK_FileNotExists() throws Exception {
    OwncloudTestResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(URI.create("/notExistingFile.txt"))
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_deleteFile_NOK_FileNotExists(owncloudFileResource);
    try {
      resourceService.delete(owncloudFileResource);
    } finally {
      check_deleteFile_NOK_FileNotExists(owncloudFileResource);
    }
  }

  protected abstract void prepare_deleteFile_NOK_FileNotExists(OwncloudTestResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_deleteFile_NOK_FileNotExists(OwncloudTestResourceImpl owncloudFileResource) throws Exception;

  @Test(expected = OwncloudResourceException.class)
  @WithOwncloudMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_NOK_OtherError() throws Exception {
    OwncloudTestResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
        .owncloudResource(
            OwncloudTestResourceImpl.builder()
                .href(URI.create("/fileWhichCausesError.txt"))
                .build())
        .testFileContent(TEST_FILE_CONTENT)
        .build();
    prepare_deleteFile_NOK_OtherError(owncloudFileResource);
    try {
      resourceService.delete(owncloudFileResource);
    } finally {
      check_deleteFile_NOK_OtherError(owncloudFileResource);
    }
  }

  protected abstract void prepare_deleteFile_NOK_OtherError(OwncloudTestResourceImpl owncloudFileResource) throws Exception;

  protected abstract void check_deleteFile_NOK_OtherError(OwncloudTestResourceImpl owncloudFileResource) throws Exception;
}
