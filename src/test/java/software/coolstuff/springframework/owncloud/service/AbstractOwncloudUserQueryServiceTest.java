package software.coolstuff.springframework.owncloud.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserQueryService.class)
public abstract class AbstractOwncloudUserQueryServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllUsers() throws Exception {
    prepareTestFindAllUsers("user1", "user2");

    List<String> users = userQueryService.findAllUsers();
    verifyServer();

    Assert.assertNotNull(users);
    Assert.assertFalse(users.isEmpty());
    Assert.assertEquals(2, users.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(users, Lists.newArrayList("user1", "user2")));
  }

  protected void prepareTestFindAllUsers(String... users) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllUsersWithFilter() throws Exception {
    prepareTestFindAllUsersWithFilter("User 1", "user1");

    List<String> users = userQueryService.findAllUsers("User 1");
    verifyServer();

    Assert.assertNotNull(users);
    Assert.assertEquals(1, users.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(users, Lists.newArrayList("user1")));
  }

  protected void prepareTestFindAllUsersWithFilter(String filter, String... users) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllGroups() throws Exception {
    prepareTestFindAllGroups("group1", "group2", "group3");

    List<String> groups = userQueryService.findAllGroups();
    verifyServer();

    Assert.assertNotNull(groups);
    Assert.assertFalse(groups.isEmpty());
    Assert.assertEquals(3, groups.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(groups, Lists.newArrayList("group1", "group2", "group3")));
  }

  protected void prepareTestFindAllGroups(String... groups) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllGroupsWithFilter() throws Exception {
    prepareTestFindAllGroupsWithFilter("p2", "group2");

    List<String> groups = userQueryService.findAllGroups("p2");
    verifyServer();

    Assert.assertNotNull(groups);
    Assert.assertEquals(1, groups.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(groups, Lists.newArrayList("group2")));
  }

  protected void prepareTestFindAllGroupsWithFilter(String filter, String... groups) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_OK() throws Exception {
    prepareTestFindAllMembersOfGroup_OK("group1", "user1");

    List<String> membersOfGroup = userQueryService.findAllMembersOfGroup("group1");
    verifyServer();

    Assert.assertNotNull(membersOfGroup);
    Assert.assertFalse(membersOfGroup.isEmpty());
    Assert.assertEquals(1, membersOfGroup.size());

    ArrayList<String> expectedUsers = Lists.newArrayList("user1");
    for (String user : membersOfGroup) {
      Assert.assertTrue(expectedUsers.contains(user));
      expectedUsers.remove(user);
    }
    Assert.assertTrue(expectedUsers.isEmpty());
  }

  protected void prepareTestFindAllMembersOfGroup_OK(String group, String... users) throws Exception {}

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_UnknownGroup() throws Exception {
    prepareTestFindAllMembersOfGroup_UnknownGroup("group4");
    userQueryService.findAllMembersOfGroup("group4");
  }

  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_GroupWithoutMembers() throws Exception {
    prepareTestFindAllMembersOfGroup_GroupWithoutMembers("group3");
    List<String> membersOfGroup = userQueryService.findAllMembersOfGroup("group3");
    Assert.assertNotNull(membersOfGroup);
    Assert.assertTrue(membersOfGroup.isEmpty());
  }

  protected void prepareTestFindAllMembersOfGroup_GroupWithoutMembers(String groupname) throws Exception {}

  @Test(expected = NullPointerException.class)
  public void testFindAllMembersOfGroup_NoGroup() {
    userQueryService.findAllMembersOfGroup(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindAllMembersOfGroup_BlankGroup() {
    userQueryService.findAllMembersOfGroup("");
  }

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindOneUser_OK() throws Exception {
    OwncloudUserDetails expectedUser = OwncloudUserDetails.builder()
        .username("user1")
        .enabled(true)
        .email("user1@example.com")
        .displayName("Mr. User 1")
        .build();
    prepareTestFindOneUser_OK(expectedUser, "group1", "group2");

    OwncloudUserDetails actualUser = userQueryService.findOneUser("user1");
    verifyServer();

    Assert.assertNotNull(actualUser);
    Assert.assertEquals(expectedUser.getUsername(), actualUser.getUsername());
    Assert.assertNull(actualUser.getPassword());
    Assert.assertEquals(expectedUser.getDisplayName(), actualUser.getDisplayName());
    Assert.assertEquals(expectedUser.getEmail(), actualUser.getEmail());

    checkAuthorities(actualUser.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithOwncloudMockUser(username = "user3", password = "password")
  public void testFindOneUser_UnknownUser() throws Exception {
    prepareTestFindOneUser_UnknownUser("user3");
    userQueryService.findOneUser("user3");
  }

  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {}

  @Test(expected = NullPointerException.class)
  public void testFindOneUser_NoUser() {
    userQueryService.findOneUser(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindOneUser_BlankUser() {
    userQueryService.findOneUser("");
  }

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllGroupsOfUser_OK() throws Exception {
    prepareTestFindAllGroupsOfUser_OK("user1", "group1", "group2");

    List<String> groups = userQueryService.findAllGroupsOfUser("user1");
    verifyServer();

    Assert.assertNotNull(groups);
    Assert.assertEquals(2, groups.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(Lists.newArrayList("group1", "group2"), groups));
  }

  protected void prepareTestFindAllGroupsOfUser_OK(String user, String... groups) throws Exception {}

  @Test
  @WithOwncloudMockUser(username = "user2", password = "password")
  public void testFindAllGroupsOfUser_OK_NoGroups() throws Exception {
    prepareTestFindAllGroupsOfUser_OK_NoGroups("user2");

    List<String> groups = userQueryService.findAllGroupsOfUser("user2");
    verifyServer();

    Assert.assertNotNull(groups);
    Assert.assertTrue(groups.isEmpty());
  }

  protected void prepareTestFindAllGroupsOfUser_OK_NoGroups(String user) throws Exception {}
}
