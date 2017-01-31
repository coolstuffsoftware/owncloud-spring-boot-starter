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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.sardine.DavResource;

import software.coolstuff.springframework.owncloud.model.OwncloudModifiableResource;

/**
 * @author mufasa1976
 */
@RunWith(SpringRunner.class)
public class OwncloudRestResourceTest {

  @Test
  public void testConstructor_WithoutDavResource() {
    OwncloudModifiableResource resource = new OwncloudRestResource(URI.create("/this/is/a/Resource"), true);
    assertThat(resource.getCreationAt()).isNotNull();
    assertThat(resource.getCreationAt()).isBeforeOrEqualsTo(new Date());
    assertThat(resource.getLastModifiedAt()).isNotNull();
    assertThat(resource.getLastModifiedAt()).isEqualTo(resource.getCreationAt());
    assertThat(resource.isDirectory()).isTrue();
    assertThat(resource.isModifiable()).isFalse();
  }

  @Test
  public void testConstrutor_WithDavResource() {
    boolean directory = true;
    URI href = URI.create("/this/is/a/Resource");
    Date creation = new Date();
    Date modified = new Date();

    DavResource davResource = mock(DavResource.class);
    when(davResource.isDirectory()).thenReturn(directory);
    when(davResource.getHref()).thenReturn(href);
    when(davResource.getCreation()).thenReturn(creation);
    when(davResource.getModified()).thenReturn(modified);

    OwncloudModifiableResource resource = new OwncloudRestResource(davResource);
    assertThat(resource.isDirectory()).isEqualTo(directory);
    assertThat(resource.isModifiable()).isNotEqualTo(directory);
    assertThat(resource.getCreationAt()).isEqualTo(creation);
    assertThat(resource.getLastModifiedAt()).isEqualTo(modified);

    verify(davResource).getHref();
    verify(davResource).isDirectory();
    verify(davResource).getModified();
  }
}
