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
package software.coolstuff.springframework.owncloud.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.resource.file.OwncloudFileResourceTest;

/**
 * This Test-Execution Listener prepares the Tests for Resources prefixed with <code>file:</code> because these Resources will also be written on Disk just before the Bean
 * <code>OwncloudResourceService</code> will be destroyed.
 *
 * @author mufasa1976
 */
@Slf4j
public class OwncloudFileResourceTestExecutionListener extends AbstractTestExecutionListener {

  private final static String PROPERTY_FILE_PREFIX = "classpath:config/application-";
  private final static String PROPERTY_FILE_SUFFIX = ".yml";

  private final static String ORIGINAL_RESOURCE = "classpath:owncloud.xml";

  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    if (OwncloudFileResourceTest.class.isAssignableFrom(testClass)) {
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
        OwncloudProperties owncloudProperties = new OwncloudProperties();
        for (Entry<String, Object> property : properties.entrySet()) {
          switch (property.getKey()) {
            case "location":
              owncloudProperties.setLocation((String) property.getValue());
              break;
            case "enable-modifications":
            case "enableModifications":
              owncloudProperties.setEnableModifications((Boolean) property.getValue());
              break;
            case "username":
              owncloudProperties.setUsername((String) property.getValue());
              break;
            case "password":
              owncloudProperties.setPassword((String) property.getValue());
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

  @Data
  private static class YamlOwncloudProperties {

    private OwncloudProperties owncloud;
  }
}
