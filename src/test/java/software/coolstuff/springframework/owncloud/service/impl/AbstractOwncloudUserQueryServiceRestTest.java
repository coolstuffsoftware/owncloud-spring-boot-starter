package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@RestClientTest(OwncloudUserQueryService.class)
public abstract class AbstractOwncloudUserQueryServiceRestTest extends AbstractOwncloudRestTest {

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  protected AbstractOwncloudServiceImpl owncloudService() {
    return (OwncloudUserQueryServiceImpl) userQueryService;
  }

  @Override
  protected String getResourcePrefix() {
    return "/queryService";
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllUsers() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/users"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllUsers"), MediaType.TEXT_XML));

    List<String> users = userQueryService.findAllUsers();
    Assert.assertNotNull(users);
    Assert.assertFalse(users.isEmpty());
    Assert.assertEquals(5, users.size());

    ArrayList<String> expectedUsers = Lists.newArrayList("user1", "user2", "user3", "user4", "user5");
    for (String user : users) {
      Assert.assertTrue(expectedUsers.contains(user));
      expectedUsers.remove(user);
    }
    Assert.assertTrue(expectedUsers.isEmpty());
  }

  protected abstract String getBasicAuthorizationHeader();

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllGroups() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllGroups"), MediaType.TEXT_XML));

    List<String> groups = userQueryService.findAllGroups();
    Assert.assertNotNull(groups);
    Assert.assertFalse(groups.isEmpty());
    Assert.assertEquals(5, groups.size());

    ArrayList<String> expectedGroups = Lists.newArrayList("group1", "group2", "group3", "group4", "group5");
    for (String group : groups) {
      Assert.assertTrue(expectedGroups.contains(group));
      expectedGroups.remove(group);
    }
    Assert.assertTrue(expectedGroups.isEmpty());
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_OK() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/groups/group1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_OK"), MediaType.TEXT_XML));

    List<String> membersOfGroup = userQueryService.findAllMembersOfGroup("group1");
    Assert.assertNotNull(membersOfGroup);
    Assert.assertFalse(membersOfGroup.isEmpty());
    Assert.assertEquals(5, membersOfGroup.size());

    ArrayList<String> expectedUsers = Lists.newArrayList("user1", "user2", "user3", "user4", "user5");
    for (String user : membersOfGroup) {
      Assert.assertTrue(expectedUsers.contains(user));
      expectedUsers.remove(user);
    }
    Assert.assertTrue(expectedUsers.isEmpty());
  }

  @Test(expected = OwncloudGroupNotFoundException.class)
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_UnknownGroup() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/groups/group1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_UnknownGroup"), MediaType.TEXT_XML));

    userQueryService.findAllMembersOfGroup("group1");
  }

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
  public void testFindOneUser_OK() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/users/user1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Information"), MediaType.TEXT_XML));
    server
        .expect(requestToWithPrefix("/cloud/users/user1/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));

    OwncloudUserDetails user = userQueryService.findOneUser("user1");
    Assert.assertNotNull(user);
    Assert.assertEquals("user1", user.getUsername());
    Assert.assertNull(user.getPassword());
    Assert.assertEquals("Mr. User 1", user.getDisplayName());
    Assert.assertEquals("user1@example.com", user.getEmail());

    checkAuthorities(user.getAuthorities(), "group1", "group2");
  }

  @Test(expected = UsernameNotFoundException.class)
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testFindOneUser_UnknownUser() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/users/user1"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_UnknownUser"), MediaType.TEXT_XML));

    userQueryService.findOneUser("user1");
  }

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
  public void testFindAllGroupsOfUser_OK() throws MalformedURLException, IOException {
    server
        .expect(requestToWithPrefix("/cloud/users/user1/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));

    List<String> groups = userQueryService.findAllGroupsOfUser("user1");
    Assert.assertNotNull(groups);
    Assert.assertEquals(2, groups.size());

    Assert.assertTrue(CollectionUtils.isEqualCollection(Lists.newArrayList("group1", "group2"), groups));
  }
}
