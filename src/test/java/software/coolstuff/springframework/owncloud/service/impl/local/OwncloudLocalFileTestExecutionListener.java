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
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.util.ResourceUtils;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;
import org.yaml.snakeyaml.Yaml;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudTestExecutionListener;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.local.file.OwncloudLocalFileTest;

/**
 * This Test-Execution Listener prepares the Tests for Resources prefixed with <code>file:</code> because these Resources will also be written on Disk just before the Bean
 * <code>OwncloudResourceService</code> will be destroyed.
 *
 * @author mufasa1976
 */
@Slf4j
public class OwncloudLocalFileTestExecutionListener extends AbstractOwncloudTestExecutionListener {

  private final static String PROPERTY_FILE_PREFIX = "classpath:config/application-";
  private final static String PROPERTY_FILE_SUFFIX = ".yml";

  private final static String ORIGINAL_RESOURCE = "classpath:owncloud.xml";

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    if (isTestClassAssignableFromOwncloudFileResourceTest(testContext)) {
      Class<?> testClass = testContext.getTestClass();
      ActiveProfiles activeProfiles = AnnotationUtils.findAnnotation(testClass, ActiveProfiles.class);
      OwncloudProperties owncloudProperties = loadProperties(activeProfiles.value());
      if (StringUtils.startsWith(owncloudProperties.getLocation(), "file:")) {
        copyResource(owncloudProperties);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private OwncloudProperties loadProperties(String[] activeProfiles) throws IOException, FileNotFoundException {
    Yaml yaml = new Yaml();
    for (String activeProfile : activeProfiles) {
      String propertyFilename = PROPERTY_FILE_PREFIX + activeProfile + PROPERTY_FILE_SUFFIX;
      log.debug("Try to parse File {} with SnakeYaml", propertyFilename);
      File propertyFile = ResourceUtils.getFile(propertyFilename);
      try (InputStream input = new FileInputStream(propertyFile)) {
        Map<String, Object> properties = ((Map<String, Map<String, Object>>) yaml.load(input)).get("owncloud");
        OwncloudProperties owncloudProperties = new OwncloudLocalProperties();
        for (Entry<String, Object> property : properties.entrySet()) {
          switch (property.getKey()) {
            case "location":
              owncloudProperties.setLocation((String) property.getValue());
              break;
            case "user-service":
            case "userService":
              continue;
            case "user-service.enable-modifications":
            case "userService.enable-modifications":
            case "user-service.enableModifications":
            case "userService.enableModifications":
              if (owncloudProperties.getUserService() == null) {
                owncloudProperties.setUserService(new OwncloudProperties.UserService());
              }
              owncloudProperties.getUserService().setEnableModifications((Boolean) property.getValue());
              break;
            default:
              throw new IllegalArgumentException("Invalid Property owncloud." + property.getKey());
          }
        }
        Validate.notNull(owncloudProperties.getLocation());
        return owncloudProperties;
      }
    }
    return null;
  }

  private void copyResource(OwncloudProperties owncloudProperties) throws IOException {
    File original = ResourceUtils.getFile(ORIGINAL_RESOURCE);
    File target = ResourceUtils.getFile(owncloudProperties.getLocation());
    target.deleteOnExit();
    try (InputStream input = new BufferedInputStream(new FileInputStream(original));
        OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
      IOUtils.copy(input, output);
    }
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    if (isTestClassAssignableFromOwncloudFileResourceTest(testContext)) {
      ApplicationContext applicationContext = testContext.getApplicationContext();

      ResourceLoader resourceLoader = applicationContext;
      OwncloudProperties properties = applicationContext.getBean(OwncloudProperties.class);
      copyClasspathResourceToFile(resourceLoader, properties);

      InitializingBean localDataService = applicationContext.getBean(OwncloudLocalUserDataServiceImpl.class);
      localDataService.afterPropertiesSet();
    }
  }

  private void copyClasspathResourceToFile(ResourceLoader resourceLoader, OwncloudProperties properties) throws IOException, FileNotFoundException {
    Resource target = resourceLoader.getResource(properties.getLocation());
    if (!(target instanceof UrlResource)) {
      throw new IllegalStateException(String.format(
          "TestClass %s implements %s but the Resource-Location %s is not of Type %s",
          this.getClass().getName(),
          OwncloudLocalFileTest.class.getName(),
          properties.getLocation(),
          UrlResource.class.getName()));
    }

    try (InputStream is = new BufferedInputStream(getSourceResource(resourceLoader).getInputStream());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(target.getFile()))) {
      log.debug("Copy Content of Classpath-Resource {} to File {}", ORIGINAL_RESOURCE, properties.getLocation());
      IOUtils.copy(is, os);
    }
  }

  public static Resource getSourceResource(ResourceLoader resourceLoader) {
    Resource resource = resourceLoader.getResource(ORIGINAL_RESOURCE);
    if (resource == null) {
      throw new IllegalStateException("Source Resource " + ORIGINAL_RESOURCE + " is not available");
    }
    return resource;
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (isTestClassAssignableFromOwncloudFileResourceTest(testContext)) {
      ApplicationContext applicationContext = testContext.getApplicationContext();

      Class<?> testClass = testContext.getTestClass();
      Method testMethod = testContext.getTestMethod();

      DisposableBean localDataService = applicationContext.getBean(OwncloudLocalUserDataServiceImpl.class);
      localDataService.destroy();

      ResourceLoader resourceLoader = applicationContext;
      OwncloudProperties properties = applicationContext.getBean(OwncloudProperties.class);
      Resource target = resourceLoader.getResource(properties.getLocation());

      boolean hasSpecificResourceTest = false;
      for (Method method : testContext.getTestClass().getMethods()) {
        // is this Method annotated by @CompareResourceAfter
        CompareResourceAfter compareResourceAfter = AnnotationUtils.findAnnotation(method, CompareResourceAfter.class);
        if (compareResourceAfter == null || !StringUtils.equals(compareResourceAfter.value(), testMethod.getName())) {
          continue;
        }

        // a Method annotated by @Test cannot also be annotated by
        // @CompareResourceAfter
        if (AnnotationUtils.findAnnotation(method, Test.class) != null) {
          log.warn("Method {} of Class {} cannot be annotated by {} and {}", method.getName(), testClass.getName(), CompareResourceAfter.class, Test.class);
          continue;
        }

        // the @CompareResourceAfter annotated Method must have exactly 2
        // Parameters of Type org.springframework.core.io.Resource
        if (method.getParameterCount() != 1) {
          log.warn("Method {} of Class {} is annotated by {} but has {} Parameters instead of 1",
              method.getName(),
              testClass.getName(),
              CompareResourceAfter.class.getName(),
              method.getParameterCount());
          continue;
        }
        boolean correctParameterTypes = true;
        for (Class<?> parameterClass : method.getParameterTypes()) {
          correctParameterTypes = correctParameterTypes && Resource.class.isAssignableFrom(parameterClass);
        }
        if (!correctParameterTypes) {
          log.warn("Method {} of Class {} (annotated by {}) must have 1 Parameter of Type {}",
              method.getName(),
              testClass.getName(),
              CompareResourceAfter.class.getName(),
              Resource.class.getName());
          continue;
        }

        log.debug("Call the Resource Comparsion Method {} on Class {}", method.getName(), testClass.getName());
        hasSpecificResourceTest = true;
        try {
          method.invoke(testContext.getTestInstance(), target);
        } catch (InvocationTargetException e) {
          throw (Exception) e.getCause();
        }
      }

      if (!hasSpecificResourceTest && ((OwncloudLocalFileTest) testContext.getTestInstance()).isCheckAllResourcesAgainstOriginal()) {
        compareResourcesWithOriginalSource(resourceLoader, target);
      }
    }
  }

  public static void compareResourcesWithOriginalSource(ResourceLoader resourceLoader, Resource target) throws Exception {
    compareResources(getSourceResource(resourceLoader), target);
  }

  public static void compareResources(Resource source, Resource target) throws Exception {
    try (InputStream inputSource = new BufferedInputStream(source.getInputStream());
        InputStream inputTarget = new BufferedInputStream(target.getInputStream())) {
      Diff diff = DiffBuilder.compare(Input.fromStream(inputSource))
          .withTest(Input.fromStream(inputTarget))
          .checkForSimilar()
          .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
          .build();
      Assert.assertFalse(diff.toString(), diff.hasDifferences());
    }
  }

  @Data
  private static class YamlOwncloudProperties {

    private OwncloudProperties owncloud;
  }
}
