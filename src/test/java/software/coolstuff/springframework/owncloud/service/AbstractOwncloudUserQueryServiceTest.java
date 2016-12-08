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

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserQueryService.class)
public abstract class AbstractOwncloudUserQueryServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  protected String getResourcePrefix() {
    return "/queryService";
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllUsers() throws Exception {
    prepareTestFindAllUsers();

    List<String> users = userQueryService.findAllUsers();
    verifyServer();

    Assert.assertNotNull(users);
    Assert.assertFalse(users.isEmpty());
    Assert.assertEquals(2, users.size());

    ArrayList<String> expectedUsers = Lists.newArrayList("user1", "user2");
    for (String user : users) {
      Assert.assertTrue(expectedUsers.contains(user));
      expectedUsers.remove(user);
    }
    Assert.assertTrue(expectedUsers.isEmpty());
  }

  protected void prepareTestFindAllUsers() throws Exception {}

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllGroups() throws Exception {
    prepareTestFindAllGroups();

    List<String> groups = userQueryService.findAllGroups();
    verifyServer();

    Assert.assertNotNull(groups);
    Assert.assertFalse(groups.isEmpty());
    Assert.assertEquals(2, groups.size());

    ArrayList<String> expectedGroups = Lists.newArrayList("group1", "group2");
    for (String group : groups) {
      Assert.assertTrue(expectedGroups.contains(group));
      expectedGroups.remove(group);
    }
    Assert.assertTrue(expectedGroups.isEmpty());
  }

  protected void prepareTestFindAllGroups() throws Exception {}

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_OK() throws Exception {
    prepareTestFindAllMembersOfGroup_OK("group1");

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

  protected void prepareTestFindAllMembersOfGroup_OK(String group) throws Exception {}

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_UnknownGroup() throws Exception {
    prepareTestFindAllMembersOfGroup_UnknownGroup("group3");
    userQueryService.findAllMembersOfGroup("group3");
  }

  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {}

  @Test(expected = NullPointerException.class)
  public void testFindAllMembersOfGroup_NoGroup() {
    userQueryService.findAllMembersOfGroup(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindAllMembersOfGroup_BlankGroup() {
    userQueryService.findAllMembersOfGroup("");
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindOneUser_OK() throws Exception {
    prepareTestFindOneUser_OK("user1");

    OwncloudUserDetails user = userQueryService.findOneUser("user1");
    verifyServer();

    Assert.assertNotNull(user);
    Assert.assertEquals("user1", user.getUsername());
    Assert.assertNull(user.getPassword());
    Assert.assertEquals("Mr. User 1", user.getDisplayName());
    Assert.assertEquals("user1@example.com", user.getEmail());

    checkAuthorities(user.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestFindOneUser_OK(String user) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockOwncloudUser(username = "user3", password = "password")
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
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllGroupsOfUser_OK() throws Exception {
    prepareTestFindAllGroupsOfUser_OK("user1");

    List<String> groups = userQueryService.findAllGroupsOfUser("user1");
    Assert.assertNotNull(groups);
    Assert.assertEquals(2, groups.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(Lists.newArrayList("group1", "group2"), groups));
  }

  protected void prepareTestFindAllGroupsOfUser_OK(String user) throws Exception {}
}
