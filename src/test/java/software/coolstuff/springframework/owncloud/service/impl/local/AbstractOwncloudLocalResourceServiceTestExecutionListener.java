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
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * @author mufasa1976
 */
@Slf4j
public class AbstractOwncloudLocalResourceServiceTestExecutionListener extends AbstractTestExecutionListener {

  private final static String PROPERTY_FILE_PREFIX = "classpath:config/application-";
  private final static String PROPERTY_FILE_SUFFIX = ".yml";

  @SuppressWarnings("unchecked")
  protected Optional<Path> getResourceLocation(String[] activeProfiles) throws IOException {
    Yaml yaml = new Yaml();
    for (String activeProfile : activeProfiles) {
      String propertyFilename = PROPERTY_FILE_PREFIX + activeProfile + PROPERTY_FILE_SUFFIX;
      log.debug("Try to parse File {} with SnakeYaml", propertyFilename);
      File propertyFile = ResourceUtils.getFile(propertyFilename);
      try (InputStream input = new FileInputStream(propertyFile)) {
        Map<String, Object> properties = ((Map<String, Map<String, Object>>) yaml.load(input)).get("owncloud");
        for (Entry<String, Object> property : properties.entrySet()) {
          switch (property.getKey()) {
            case "resource-service":
            case "resourceService":
              Map<String, Object> resourceProperties = (Map<String, Object>) property.getValue();
              if (resourceProperties.containsKey("location")) {
                return Optional.of(Paths.get((String) resourceProperties.get("location")));
              }
              break;
            default:
              continue;
          }
        }
      }
    }
    return Optional.empty();
  }

}
