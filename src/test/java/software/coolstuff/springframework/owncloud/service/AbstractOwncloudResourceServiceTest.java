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

import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
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
@ComponentScan
@RestClientTest(OwncloudResourceService.class)
public abstract class AbstractOwncloudResourceServiceTest {

  @Autowired
  private OwncloudResourceService resourceService;

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void testImplementationClass() {
    assertThat(resourceService).isNotNull();
    assertThat(resourceService.getClass()).isAssignableFrom(getImplementationClass());
  }

  protected abstract Class<? extends OwncloudResourceService> getImplementationClass();

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_listRoot_OK() throws Exception {
    String uuid = UUID.randomUUID().toString();
    List<OwncloudTestResourceImpl> expected = Lists.newArrayList(
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
                .backendETag(uuid)
                .backendName("resource1")
                .eTag(uuid)
                .href(URI.create("/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_OCTET_STREAM)
                .name("resource1")
                .build())
            .contentLength(Long.valueOf(14))
            .build());
    prepare_listRoot_OK(expected);
    List<OwncloudResource> resources = resourceService.listRoot();
    compareResult(resources, expected);
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

    private Long contentLength;

    @Builder(builderMethodName = "fileBuilder")
    private OwncloudTestFileResourceImpl(OwncloudTestResourceImpl owncloudResource, Long contentLength) {
      super(
          owncloudResource.getHref(),
          owncloudResource.getName(),
          owncloudResource.getBackendName(),
          owncloudResource.getLastModifiedAt(),
          owncloudResource.getMediaType(),
          owncloudResource.getETag(),
          owncloudResource.getBackendETag());
      this.contentLength = contentLength;
    }
  }

  protected abstract void prepare_listRoot_OK(List<OwncloudTestResourceImpl> expectedResources) throws Exception;

  private void compareResult(List<OwncloudResource> actualResources, List<OwncloudTestResourceImpl> expectedResources) {
    assertThat(actualResources).hasSameSizeAs(expectedResources);
    for (OwncloudResource actualResource : actualResources) {
      OwncloudResource expectedResource = getExpectedResource(actualResource, expectedResources);
      assertThat(actualResource).isEqualToComparingFieldByField(expectedResource);
    }
  }

  private OwncloudResource getExpectedResource(OwncloudResource actualResource, List<OwncloudTestResourceImpl> expectedResources) {
    for (Iterator<OwncloudTestResourceImpl> expectedResourceIterator = expectedResources.iterator(); expectedResourceIterator.hasNext();) {
      OwncloudTestResourceImpl expectedResource = expectedResourceIterator.next();
      if (actualResource.equals(expectedResource)) {
        expectedResourceIterator.remove();
        return expectedResource;
      }
    }
    throw new IllegalStateException("No expected Resource found for actual Resource " + actualResource);
  }

  @Test
  @WithMockUser(username = "user", password = "s3cr3t")
  public void test_list_OK() throws Exception {
    URI searchPath = URI.create("/directory/directory/");
    String uuidSearchPath = UUID.randomUUID().toString();
    String uuidResource = UUID.randomUUID().toString();
    String uuidSuperPath = UUID.randomUUID().toString();
    List<OwncloudTestResourceImpl> expected = Lists.newArrayList(
        OwncloudTestResourceImpl.builder()
            .backendETag(uuidSearchPath)
            .backendName("directory")
            .eTag(uuidSearchPath)
            .href(appendPath(searchPath, "/"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name(".")
            .build(),
        OwncloudTestFileResourceImpl.fileBuilder()
            .owncloudResource(OwncloudTestResourceImpl.builder()
                .backendETag(uuidResource)
                .backendName("resource1")
                .eTag(uuidResource)
                .href(appendPath(searchPath, "/resource1"))
                .lastModifiedAt(new Date())
                .mediaType(MediaType.APPLICATION_OCTET_STREAM)
                .name("resource1")
                .build())
            .contentLength(Long.valueOf(14))
            .build(),
        OwncloudTestResourceImpl.builder()
            .backendETag(uuidSuperPath)
            .backendName("directory")
            .eTag(uuidSuperPath)
            .href(appendPath(searchPath, "/../"))
            .lastModifiedAt(new Date())
            .mediaType(OwncloudUtils.getDirectoryMediaType())
            .name("..")
            .build());
    prepare_list_OK(searchPath, expected);
    List<OwncloudResource> resources = resourceService.list(searchPath);
    compareResult(resources, expected);
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
}
