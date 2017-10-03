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

import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

public class OwncloudLocalServiceInitializerTest {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties owncloudProperties;

  @Test(expected = IllegalStateException.class)
  public void testCheckGroupReferences_NOK() throws Exception {
    Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.xml();
    InitializingBean localDataService = new OwncloudLocalUserDataServiceImpl(builder, resourceLoader, owncloudProperties) {

      @Override
      public void afterPropertiesSet() throws Exception {
        checkGroupReferences(
            OwncloudLocalUserData.builder()
                                 .user(
                                     OwncloudLocalUserData.User.builder()
                                                               .group("group1")
                                                               .group("group2")
                                                               .build())
                                 .group("group1")
                                 .build());
      }

    };
    localDataService.afterPropertiesSet();
  }
}
