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
package software.coolstuff.springframework.owncloud.service.impl.local.file;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalFileTestExecutionListener;

/**
 * A markup Interface to inform the Method {@link OwncloudLocalFileTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)} to copy the Classpath Resource
 * <code>classpath:/owncloud.xml</code> to the File-Location set within the application.yml File.
 *
 * This Interface will also be used by <code>AbstractOwncloudResourceTest.setUp()</code> to always load the Resource-Service with the Data from <code>classpath:/owncloud.xml</code> just right before
 * any Test starts. This will ensure, that every Testcase has the same Information which it builds upon.
 *
 * Also right after a Test the Method <code>AbstractOwncloudResourceTest.tearDown()</code> will call any Methods annotated by {@link CompareResourceAfter} to compare the written Data with the one, the
 * Tests assumes (normally on no Modification with the Original Source <code>classpath:/owncloud.xml</code>)
 *
 * @author mufasa1976
 * @see OwncloudLocalFileTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)
 *
 */
public interface OwncloudLocalFileTest {

  default boolean isCheckAllResourcesAgainstOriginal() {
    return true;
  }

  default Resource getResourceOf(ResourceLoader resourceLoader, String testCase) {
    if (!(this instanceof OwncloudLocalModifyingFileTest)) {
      return null;
    }

    OwncloudLocalModifyingFileTest modifyingFileResourceTest = (OwncloudLocalModifyingFileTest) this;

    String path = "/";
    if (StringUtils.isNotBlank(modifyingFileResourceTest.getResourcePrefix())) {
      if (StringUtils.startsWith(modifyingFileResourceTest.getResourcePrefix(), "/")) {
        path = StringUtils.appendIfMissing(modifyingFileResourceTest.getResourcePrefix(), "/");
      } else {
        path += StringUtils.appendIfMissing(modifyingFileResourceTest.getResourcePrefix(), "/");
      }
    }

    return resourceLoader.getResource("classpath:" + path + testCase + ".xml");
  }

}
