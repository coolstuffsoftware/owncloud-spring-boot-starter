/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service;

import com.google.common.collect.Lists;
import lombok.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.exception.resource.*;
import software.coolstuff.springframework.owncloud.model.OwncloudFileResource;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    properties = "debug=true",
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
                                                        .lastModifiedAt(LocalDateTime.now())
                                                        .mediaType(OwncloudUtils.getDirectoryMediaType())
                                                        .name(".")
                                                        .build();
    OwncloudResource actual = prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(expected);
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
  }

  protected OwncloudResource prepare_OwncloudTestResourceImpl_equalsTo_OwncloudResourceImpl(OwncloudResource expected) throws Exception {
    return null;
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_listRoot_OK() throws Exception {
    String eTag = UUID.randomUUID().toString();
    List<OwncloudTestResourceImpl> expectedResources = Lists.newArrayList(
        OwncloudTestResourceImpl.builder()
                                .backendETag(UUID.randomUUID().toString())
                                .backendName("user")
                                .href(URI.create("/"))
                                .lastModifiedAt(LocalDateTime.now())
                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                .name(".")
                                .build(),
        OwncloudTestFileResourceImpl.fileBuilder()
                                    .owncloudResource(OwncloudTestFileResourceImpl.builder()
                                                                                  .backendETag(eTag)
                                                                                  .backendName("resource1")
                                                                                  .eTag(eTag)
                                                                                  .href(URI.create("/resource1"))
                                                                                  .lastModifiedAt(LocalDateTime.now())
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
    private LocalDateTime lastModifiedAt;
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

  protected void prepare_listRoot_OK(List<OwncloudTestResourceImpl> expectedResources) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
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
                                .lastModifiedAt(LocalDateTime.now())
                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                .name(".")
                                .build(),
        OwncloudTestFileResourceImpl.fileBuilder()
                                    .owncloudResource(OwncloudTestResourceImpl.builder()
                                                                              .backendETag(eTagFileResource)
                                                                              .backendName("resource1")
                                                                              .eTag(eTagFileResource)
                                                                              .href(appendPath(searchPath, "/resource1"))
                                                                              .lastModifiedAt(LocalDateTime.now())
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
                                .lastModifiedAt(LocalDateTime.now())
                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                .name("..")
                                .build());
    prepare_list_OK(searchPath, expectedResources);
    List<OwncloudResource> resources = resourceService.list(searchPath);
    assertThat(resources).containsOnlyElementsOf(expectedResources);
  }

  protected void prepare_list_OK(URI searchPath, List<OwncloudTestResourceImpl> expectedOwncloudResources) throws Exception {
  }

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
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_list_NOK_FileNotFound() throws Exception {
    URI searchPath = URI.create("/unknown");
    prepare_list_NOK_FileNotFound(searchPath);
    resourceService.list(searchPath);
    fail("Expected Exception " + OwncloudResourceNotFoundException.class.getName() + " has not been thrown");
  }

  protected void prepare_list_NOK_FileNotFound(URI searchPath) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_findFile_OK() throws Exception {
    URI searchPath = URI.create("/file.txt");
    String eTag = UUID.randomUUID().toString();
    OwncloudTestFileResourceImpl expectedResource = OwncloudTestFileResourceImpl.fileBuilder()
                                                                                .owncloudResource(OwncloudTestResourceImpl.builder()
                                                                                                                          .backendETag(eTag)
                                                                                                                          .backendName("file.txt")
                                                                                                                          .eTag(eTag)
                                                                                                                          .href(searchPath)
                                                                                                                          .lastModifiedAt(LocalDateTime.now())
                                                                                                                          .mediaType(MediaType.TEXT_PLAIN)
                                                                                                                          .name("file.txt")
                                                                                                                          .build())
                                                                                .testFileContent(TEST_FILE_CONTENT)
                                                                                .build();
    prepare_findFile_OK(searchPath, expectedResource);
    Optional<OwncloudResource> resource = resourceService.find(searchPath);
    assertThat(resource).isNotNull();
    assertThat(resource).isNotEmpty();
    assertThat(resource.get()).isEqualTo(expectedResource);
  }

  protected void prepare_findFile_OK(URI searchPath, OwncloudTestFileResourceImpl expectedResource) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_findFile_NotExists() throws Exception {
    URI searchPath = URI.create("/unknownFile.txt");
    prepare_findFile_NotExists(searchPath);
    Optional<OwncloudResource> resource = resourceService.find(searchPath);
    assertThat(resource).isNotNull();
    assertThat(resource).isEmpty();
  }

  protected void prepare_findFile_NotExists(URI searchPath) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_findRootDirectory_OK() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudTestResourceImpl expected = OwncloudTestResourceImpl.builder()
                                                                .backendETag(UUID.randomUUID().toString())
                                                                .backendName(authentication.getName())
                                                                .eTag(null)
                                                                .href(URI.create("/"))
                                                                .lastModifiedAt(LocalDateTime.now())
                                                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                                                .name("/")
                                                                .build();
    prepare_findRootDirectory_OK(expected);
    Optional<OwncloudResource> actual = resourceService.find(null);
    assertThat(actual).isNotNull();
    assertThat(actual).isNotEmpty();
    assertThat(actual.get()).isEqualTo(expected);
  }

  protected void prepare_findRootDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
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
      input.close(); // Call Close twice to check, if we don't get any Thread-Deadlocks
    }
    check_getInputStream_OK();
  }

  protected void prepare_getInputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_getInputStream_OK() throws Exception {
  }

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
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

  protected void prepare_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_getInputStream_NOK_FileNotFound(OwncloudTestFileResourceImpl owncloudFileResource) {
    fail("OwncloudResourceNotFoundException should be thrown");
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
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
      output.close(); // Call Close twice to check, if we don't get any Thread-Deadlocks
    } finally {
      check_getOutputStream_OK(owncloudFileResource);
    }
  }

  protected void prepare_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_getOutputStream_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  @Test(expected = OwncloudResourceException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
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

  protected void prepare_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_getOutputStream_NOK_Unauthorized(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_OK_CreateNewFile() throws Exception {
    URI href = URI.create("/createdFile.txt");
    MediaType mediaType = MediaType.TEXT_PLAIN;
    prepare_getOutputStream_OK_CreateNewFile(href, mediaType, TEST_FILE_CONTENT);
    try (OutputStream output = resourceService.getOutputStream(href, mediaType)) {
      IOUtils.write(TEST_FILE_CONTENT, output, Charset.forName("utf8"));
    } finally {
      check_getOutputStream_OK_CreateNewFile(href, mediaType, TEST_FILE_CONTENT);
    }
  }

  protected void prepare_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
  }

  protected void check_getOutputStream_OK_CreateNewFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
  }

  @Test(expected = OwncloudNoFileResourceException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_NOK_ResourceIsDirectory() throws Exception {
    URI href = URI.create("/testDirectory");
    prepare_getOutputStream_NOK_ResourceIsDirectory(href);
    try (OutputStream output = resourceService.getOutputStream(href, MediaType.TEXT_PLAIN)) {
      output.write(1);
    } finally {
      check_getOutputStream_NOK_ResourceIsDirectory(href);
    }
    fail("OwncloudNoFileResourceException should be thrown");
  }

  protected void prepare_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
  }

  protected void check_getOutputStream_NOK_ResourceIsDirectory(URI href) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStream_OK_OverwriteFile() throws Exception {
    URI href = URI.create("/existingFile.txt");
    MediaType mediaType = MediaType.TEXT_PLAIN;
    prepare_getOutputStream_OK_OverwriteFile(href, mediaType, TEST_FILE_CONTENT);
    try (OutputStream output = resourceService.getOutputStream(href, mediaType)) {
      IOUtils.write(TEST_FILE_CONTENT, output, Charset.forName("utf8"));
    } finally {
      check_getOutputStream_OK_OverwriteFile(href, mediaType, TEST_FILE_CONTENT);
    }
  }

  protected void prepare_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
  }

  protected void check_getOutputStream_OK_OverwriteFile(URI href, MediaType mediaType, String testFileContent) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_OK() throws Exception {
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
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

  protected void prepare_deleteFile_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_deleteFile_OK(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  @Test(expected = OwncloudResourceNotFoundException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_NOK_FileNotExists() throws Exception {
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
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

  protected void prepare_deleteFile_NOK_FileNotExists(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_deleteFile_NOK_FileNotExists(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  @Test(expected = OwncloudResourceException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteFile_NOK_OtherError() throws Exception {
    OwncloudTestFileResourceImpl owncloudFileResource = OwncloudTestFileResourceImpl.fileBuilder()
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

  protected void prepare_deleteFile_NOK_OtherError(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  protected void check_deleteFile_NOK_OtherError(OwncloudTestFileResourceImpl owncloudFileResource) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_deleteDirectory_OK() throws Exception {
    OwncloudTestResourceImpl owncloudResource = OwncloudTestResourceImpl.builder()
                                                                        .href(URI.create("/directory/subDirectory"))
                                                                        .mediaType(OwncloudUtils.getDirectoryMediaType())
                                                                        .build();
    prepare_deleteDirectory_OK(owncloudResource);
    resourceService.delete(owncloudResource);
    check_deleteDirectory_OK(owncloudResource);
  }

  protected void prepare_deleteDirectory_OK(OwncloudTestResourceImpl owncloudResource) throws Exception {
  }

  protected void check_deleteDirectory_OK(OwncloudTestResourceImpl owncloudResource) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_createDirectory_OK() throws Exception {
    String name = "directory";
    URI uri = URI.create('/' + name + '/');
    String eTag = UUID.randomUUID().toString();
    OwncloudTestResourceImpl expected = OwncloudTestResourceImpl.builder()
                                                                .href(uri)
                                                                .backendName(name)
                                                                .name(name)
                                                                .backendETag(eTag)
                                                                .eTag(eTag)
                                                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                                                .build();
    prepare_createDirectory_OK(expected);
    OwncloudResource actual = resourceService.createDirectory(uri);
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
    check_createDirectory_OK(expected);
  }

  protected void prepare_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
  }

  protected void check_createDirectory_OK(OwncloudTestResourceImpl expectedResource) throws Exception {
  }

  @Test(expected = OwncloudNoDirectoryResourceException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_createDirectory_NOK_AlreadyExistsAsFile() throws Exception {
    URI uri = URI.create("/directory/");
    prepare_createDirectory_NOK_AlreadyExistsAsFile(uri);
    try {
      resourceService.createDirectory(uri);
    } finally {
      check_createDirectory_NOK_AlreadyExistsAsFile(uri);
    }
  }

  protected void prepare_createDirectory_NOK_AlreadyExistsAsFile(URI uri) throws Exception {
  }

  protected void check_createDirectory_NOK_AlreadyExistsAsFile(URI uri) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_createDirectory_OK_AlreadyExistsAsDirectory() throws Exception {
    URI uri = URI.create("/directory/");
    OwncloudTestResourceImpl expected = OwncloudTestResourceImpl.builder()
                                                                .href(uri)
                                                                .mediaType(OwncloudUtils.getDirectoryMediaType())
                                                                .build();
    prepare_createDirectory_OK_AlreadyExistsAsDirectory(expected);
    OwncloudResource actual = resourceService.createDirectory(uri);
    assertThat(actual).isNotNull();
    assertThat(actual).isEqualTo(expected);
    check_createDirectory_OK_AlreadyExistsAsDirectory(expected);
  }

  protected void prepare_createDirectory_OK_AlreadyExistsAsDirectory(OwncloudTestResourceImpl expected) throws Exception {
  }

  protected void check_createDirectory_OK_AlreadyExistsAsDirectory(OwncloudTestResourceImpl expected) throws Exception {
  }

  @Test(expected = OwncloudQuotaExceededException.class)
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getOutputStram_NOK_FileTooBig() throws Exception {
    URI uri = URI.create("/tooBig.txt");
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    String testFileContent = RandomStringUtils.random(1025);
    prepare_getOutputStram_NOK_FileTooBig(uri, mediaType, testFileContent);
    try (OutputStream output = resourceService.getOutputStream(uri, mediaType)) {
      IOUtils.write(testFileContent, output, Charset.forName("utf8"));
    } finally {
      check_getOutputStram_NOK_FileTooBig(uri);
    }
  }

  protected void prepare_getOutputStram_NOK_FileTooBig(URI uri, MediaType mediaType, String testFileContent) throws Exception {
  }

  protected void check_getOutputStram_NOK_FileTooBig(URI uri) throws Exception {
  }

  @Test
  @WithMockUser(username = "user1", password = "s3cr3t")
  public void test_getQuota_NoFiles() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudQuota expected = TestOwncloudQuota.builder()
                                              .username(authentication.getName())
                                              .total(1024)
                                              .used(0)
                                              .free(1024)
                                              .relative(0.0f)
                                              .build();
    prepare_getQuota_NoFiles(expected);
    OwncloudQuota quota = resourceService.getQuota();
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expected, "username", "free", "used", "total", "relative");
  }

  @Data
  @Builder
  private static class TestOwncloudQuota implements OwncloudQuota {
    private final String username;
    private final long free;
    private final long used;
    private final long total;
    private final float relative;
  }

  protected void prepare_getQuota_NoFiles(OwncloudQuota expected) throws Exception {
  }

  public void test_getQuota_OneFile() throws Exception {
    URI uri = URI.create("/testFile.txt");
    MediaType mediaType = MediaType.TEXT_PLAIN;
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OwncloudQuota expectedFirst = TestOwncloudQuota.builder()
                                                   .username(authentication.getName())
                                                   .total(1024)
                                                   .used(0)
                                                   .free(1024)
                                                   .relative(0.0f)
                                                   .build();
    OwncloudQuota expectedSecond = TestOwncloudQuota.builder()
                                                    .username(authentication.getName())
                                                    .total(1024)
                                                    .used(TEST_FILE_CONTENT.length())
                                                    .free(1024 - TEST_FILE_CONTENT.length())
                                                    .relative(TEST_FILE_CONTENT.length() / 1024 * 100)
                                                    .build();
    prepare_getQuota_OneFile(uri, mediaType, TEST_FILE_CONTENT, expectedFirst, expectedSecond);

    OwncloudQuota actual = resourceService.getQuota();
    assertThat(actual)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expectedFirst, "username", "total", "free", "used", "relative");

    try (OutputStream output = resourceService.getOutputStream(uri, mediaType)) {
      IOUtils.write(TEST_FILE_CONTENT, output, Charset.forName("utf8"));
    }

    actual = resourceService.getQuota();
    assertThat(actual)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expectedSecond, "username", "total", "free", "used", "relative");
  }

  protected void prepare_getQuota_OneFile(URI uri, MediaType mediaType, String testFileContent, OwncloudQuota expectedFirst, OwncloudQuota expectedSecond) throws Exception {
  }
}
