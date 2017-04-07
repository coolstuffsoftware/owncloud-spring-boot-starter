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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.test.context.support.WithMockUser;

import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupQueryService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserQueryService.class)
public abstract class AbstractOwncloudGroupQueryServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudGroupQueryService groupQueryService;

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllGroups() throws Exception {
    prepareTestFindAllGroups("group1", "group2", "group3");

    List<String> groups = groupQueryService.findAll();
    verifyServer();

    assertThat(groups)
        .isNotNull()
        .containsOnly("group1", "group2", "group3");
  }

  protected void prepareTestFindAllGroups(String... groups) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllGroupsWithFilter() throws Exception {
    prepareTestFindAllGroupsWithFilter("p2", "group2");

    List<String> groups = groupQueryService.findAll("p2");
    verifyServer();

    assertThat(groups)
        .isNotNull()
        .containsOnly("group2");
  }

  protected void prepareTestFindAllGroupsWithFilter(String filter, String... groups) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_OK() throws Exception {
    prepareTestFindAllMembersOfGroup_OK("group1", "user1");

    List<String> membersOfGroup = groupQueryService.findAllUsers("group1");
    verifyServer();

    assertThat(membersOfGroup)
        .isNotNull()
        .containsOnly("user1");
  }

  protected void prepareTestFindAllMembersOfGroup_OK(String group, String... users) throws Exception {}

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_UnknownGroup() throws Exception {
    prepareTestFindAllMembersOfGroup_UnknownGroup("group4");
    groupQueryService.findAllUsers("group4");
  }

  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_GroupWithoutMembers() throws Exception {
    prepareTestFindAllMembersOfGroup_GroupWithoutMembers("group3");
    List<String> membersOfGroup = groupQueryService.findAllUsers("group3");
    assertThat(membersOfGroup)
        .isNotNull()
        .isEmpty();;
  }

  protected void prepareTestFindAllMembersOfGroup_GroupWithoutMembers(String groupname) throws Exception {}

  @Test(expected = NullPointerException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_NoGroup() {
    groupQueryService.findAllUsers(null);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_BlankGroup() {
    groupQueryService.findAllUsers("");
  }

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllGroupsOfUser_OK() throws Exception {
    prepareTestFindAllGroupsOfUser_OK("user1", "group1", "group2");

    List<String> groups = groupQueryService.findAllGroups("user1");
    verifyServer();

    assertThat(groups)
        .isNotNull()
        .containsOnly("group1", "group2");
  }

  protected void prepareTestFindAllGroupsOfUser_OK(String user, String... groups) throws Exception {}

  @Test
  @WithMockUser(username = "user2", password = "password")
  public void testFindAllGroupsOfUser_OK_NoGroups() throws Exception {
    prepareTestFindAllGroupsOfUser_OK_NoGroups("user2");

    List<String> groups = groupQueryService.findAllGroups("user2");
    verifyServer();

    assertThat(groups)
        .isNotNull()
        .isEmpty();
  }

  protected void prepareTestFindAllGroupsOfUser_OK_NoGroups(String user) throws Exception {}
}
