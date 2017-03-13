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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserModificationService.class)
@ActiveProfiles("REST-NO-MODIFICATION-USER-SERVICE")
public class OwncloudRestUserModificationServiceNoModificationTest extends AbstractOwncloudServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) userModificationService;
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user", password = "s3cr3t")
  public void testSaveUser() {
    userModificationService.saveUser(new OwncloudModificationUser("user99"));
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user", password = "s3cr3t")
  public void testCreateGroup() {
    userModificationService.createGroup("shouldBeAccessDenied");
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user", password = "s3cr3t")
  public void testDeleteGroup() {
    userModificationService.deleteGroup("shouldBeAccessDenied");
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user", password = "s3cr3t")
  public void testDeleteUser() {
    userModificationService.deleteUser("shouldBeAccessDenied");
  }

}
