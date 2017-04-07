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

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudGroupServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;

@ActiveProfiles("REST-USER-SERVICE")
public class OwncloudRestGroupServiceTest extends AbstractOwncloudGroupServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudGroupService groupService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) groupService;
  }

  @Override
  protected void prepareTestCreateGroup_OK(String groupname) throws Exception {
    respondSuccess(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/" + groupname)
            .build());
  }

  @Override
  protected void prepareTestCreateGroup_NOK_OwncloudGroupAlreadyExists(String groupname) throws Exception {
    respondFailure(RestRequest.builder().method(POST).url("/cloud/groups/" + groupname).build(), 102);
  }

  @Override
  protected void prepareTestDeleteGroup_OK(String groupname) throws Exception {
    respondSuccess(RestRequest.builder().method(DELETE).url("/cloud/groups/" + groupname).build());
  }

  @Override
  protected void prepareTestDeleteGroup_NOK_GroupNotFound(String groupname) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/" + groupname)
            .build(),
        101);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_IllegalArgumentException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        101);
    groupService.create("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_CannotCreateGroup() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        103);
    groupService.create("group4");
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_AccessDeniedException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        997);
    groupService.create("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testCreateGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/groups/group4")
            .build(),
        999);
    groupService.create("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_CannotDeleteGroup() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        102);
    groupService.delete("group4");
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_AccessDeniedException() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        997);
    groupService.delete("group4");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testDeleteGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(DELETE)
            .url("/cloud/groups/group4")
            .build(),
        999);
    groupService.delete("group4");
  }

}
