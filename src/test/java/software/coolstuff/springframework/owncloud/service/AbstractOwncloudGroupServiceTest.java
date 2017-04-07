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
package software.coolstuff.springframework.owncloud.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.test.context.support.WithMockUser;

import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudGroupService.class)
public abstract class AbstractOwncloudGroupServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudGroupService groupService;

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_OK() throws Exception {
    prepareTestCreateGroup_OK("group4");
    groupService.create("group4");
  }

  protected void prepareTestCreateGroup_OK(String groupname) throws Exception {}

  @Test(expected = OwncloudGroupAlreadyExistsException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_OwncloudGroupAlreadyExists() throws Exception {
    prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists("group1");
    groupService.create("group1");
  }

  protected void prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists(String groupname) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_OK() throws Exception {
    prepareTestDeleteGroup_OK("group1");
    groupService.delete("group1");
  }

  protected void prepareTestDeleteGroup_OK(String groupname) throws Exception {}

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_GroupNotFound() throws Exception {
    prepareTestDeleteGroup_NOK_GroupNotFound("group4");
    groupService.delete("group4");
  }

  protected void prepareTestDeleteGroup_NOK_GroupNotFound(String groupname) throws Exception {}
}
