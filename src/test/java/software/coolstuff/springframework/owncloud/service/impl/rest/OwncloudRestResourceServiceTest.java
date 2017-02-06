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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.github.sardine.Sardine;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudResourceServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService;

@ActiveProfiles("REST-RESOURCE-SERVICE")
public class OwncloudRestResourceServiceTest extends AbstractOwncloudResourceServiceTest {

  @MockBean
  private SardineCacheLoader sardineCacheLoader;

  @Mock
  private Sardine sardine;

  @Autowired
  private OwncloudResourceService resourceService;

  @Before
  public void setUp() throws Exception {
    Mockito.when(sardineCacheLoader.createInstance(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(sardine);
  }

  @Test
  public void testXXX() throws Exception {}
}
