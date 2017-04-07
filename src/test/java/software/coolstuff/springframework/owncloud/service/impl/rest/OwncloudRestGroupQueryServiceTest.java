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

import static org.springframework.http.HttpMethod.GET;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriUtils;

import software.coolstuff.springframework.owncloud.service.AbstractOwncloudGroupQueryServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupQueryService;

@ActiveProfiles("REST-USER-SERVICE")
public class OwncloudRestGroupQueryServiceTest extends AbstractOwncloudGroupQueryServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudGroupQueryService groupQueryService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) groupQueryService;
  }

  @Override
  protected void prepareTestFindAllGroups(String... groups) throws Exception {
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestFindAllGroupsWithFilter(String filter, String... groups) throws Exception {
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups?search=" + UriUtils.encode(filter, "UTF8"))
            .build(),
        groups);
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_OK(String group, String... users) throws Exception {
    respondUsers(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/" + group)
            .build(),
        users);
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/" + group)
            .build(),
        998,
        "The requested group could not be found");
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_GroupWithoutMembers(String groupname) throws Exception {
    respondUsers(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/" + groupname)
            .build(),
        new String[] {});
  }

  @Override
  protected void prepareTestFindAllGroupsOfUser_OK(String user, String... groups) throws Exception {
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user + "/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestFindAllGroupsOfUser_OK_NoGroups(String user) throws Exception {
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user + "/groups")
            .build(),
        new String[] {});
  }

  @Test(expected = BadCredentialsException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testBadCredentialsBy401() throws Exception {
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups")
            .build(),
        HttpStatus.UNAUTHORIZED);

    groupQueryService.findAll();
  }

  @Test(expected = HttpClientErrorException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testNotFound() throws Exception {
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups")
            .build(),
        HttpStatus.NOT_FOUND);

    groupQueryService.findAll();
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_NOK_AccessDenied() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/group1")
            .build(),
        997);
    groupQueryService.findAllUsers("group1");
  }

  @Test(expected = IllegalStateException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/group1")
            .build(),
        999);
    groupQueryService.findAllUsers("group1");
  }

}
