package software.coolstuff.springframework.owncloud.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.AbstractOwncloudTest;
import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.config.OwncloudResourceFileTest;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;

@Slf4j
public abstract class AbstractOwncloudResourceTest extends AbstractOwncloudTest {

  private final static String ORIGINAL_RESOURCE = "classpath:/owncloud.xml";

  @Rule
  public TestName testName = new TestName();

  @Autowired
  private OwncloudProperties owncloudProperties;

  @Autowired
  private OwncloudResourceService resourceService;

  @Autowired
  private ResourceLoader resourceLoader;

  @Before
  public void setUp() throws Exception {
    if (this instanceof OwncloudResourceFileTest) {
      copyClasspathResourceToFile();
      resourceService.afterPropertiesSet();
    }
  }

  private void copyClasspathResourceToFile() throws IOException, FileNotFoundException {
    Resource target = resourceLoader.getResource(owncloudProperties.getLocation());
    if (!(target instanceof UrlResource)) {
      throw new IllegalStateException(String.format("TestClass %s implements %s but the Resource-Location %s is not of Type %s", this.getClass().getName(),
          OwncloudResourceFileTest.class.getName(), owncloudProperties.getLocation(), UrlResource.class.getName()));
    }

    try (InputStream is = new BufferedInputStream(getSourceResource().getInputStream());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(target.getFile()))) {
      log.debug("Copy Content of Classpath-Resource {} to File {}", ORIGINAL_RESOURCE, owncloudProperties.getLocation());
      IOUtils.copy(is, os);
    }
  }

  protected Resource getSourceResource() {
    Resource resource = resourceLoader.getResource(ORIGINAL_RESOURCE);
    if (resource == null) {
      throw new IllegalStateException("Source Resource " + ORIGINAL_RESOURCE + " is not available");
    }
    return resource;
  }

  protected void compareResources(Resource target) throws Exception {
    try (InputStream isSource = new BufferedInputStream(getSourceResource().getInputStream());
        InputStream isTarget = new BufferedInputStream(target.getInputStream())) {
      Diff diff = DiffBuilder.compare(Input.fromStream(isSource))
          .withTest(Input.fromStream(isTarget))
          .build();
      Assert.assertFalse(diff.toString(), diff.hasDifferences());
    }
  }

  @After
  public void tearDown() throws Exception {
    if (this instanceof OwncloudResourceFileTest) {
      resourceService.destroy();
      Resource target = resourceLoader.getResource(owncloudProperties.getLocation());

      for (Method method : this.getClass().getMethods()) {
        // is this Method annotated by @CompareResourceAfter
        CompareResourceAfter compareResourceAfter = AnnotationUtils.findAnnotation(method, CompareResourceAfter.class);
        if (compareResourceAfter == null || !StringUtils.equals(compareResourceAfter.value(), testName.getMethodName())) {
          continue;
        }

        // a Method annotated by @Test cannot also be annotated by @CompareResourceAfter
        if (AnnotationUtils.findAnnotation(method, Test.class) != null) {
          log.warn("Method {} of Class {} cannot be annotated by {} and {}", method.getName(), this.getClass().getName(), CompareResourceAfter.class, Test.class);
          continue;
        }

        // the @CompareResourceAfter annotated Method must have exactly 2 Parameters of Type org.springframework.core.io.Resource
        if (method.getParameterCount() != 1) {
          log.warn("Method {} of Class {} is annotated by {} but has {} Parameters instead of 1",
              method.getName(), this.getClass().getName(), CompareResourceAfter.class.getName(), method.getParameterCount());
          continue;
        }
        boolean correctParameterTypes = true;
        for (Class<?> parameterClass : method.getParameterTypes()) {
          correctParameterTypes = correctParameterTypes && Resource.class.isAssignableFrom(parameterClass);
        }
        if (!correctParameterTypes) {
          log.warn("Method {} of Class {} (annotated by {}) must have 1 Parameter of Type {}",
              method.getName(), this.getClass(), CompareResourceAfter.class.getName(), Resource.class.getName());
          continue;
        }

        log.debug("Call the Resource Comparsion Method {} on Class {}", method.getName(), this.getClass().getName());
        method.invoke(this, target);
      }
    }
  }
}
