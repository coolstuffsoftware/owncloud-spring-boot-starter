package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriUtils;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;

public abstract class AbstractOwncloudUserQueryServiceRestTest extends AbstractOwncloudUserQueryServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserQueryServiceImpl userQueryService;

  @Override
  public final AbstractOwncloudServiceImpl owncloudService() {
    return userQueryService;
  }

  @Override
  protected void prepareTestFindAllUsers(String... users) throws Exception {
    respondUsers(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users")
            .build(),
        users);
  }

  @Override
  protected void prepareTestFindAllUsersWithFilter(String filter, String... users) throws Exception {
    respondUsers(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users?search=" + UriUtils.encode(filter, "UTF8"))
            .build(),
        users);
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
  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + expectedUser.getUsername())
            .build(),
        expectedUser.isEnabled(),
        expectedUser.getEmail(),
        expectedUser.getDisplayName());
    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + expectedUser.getUsername() + "/groups")
            .build(),
        groups);
  }

  @Override
  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + user)
            .build(),
        998,
        "The requested user could not be found");
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
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testBadCredentialsBy401() throws Exception {
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups")
            .build(),
        HttpStatus.UNAUTHORIZED);

    userQueryService.findAllGroups();
  }

  @Test(expected = HttpClientErrorException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testNotFound() throws Exception {
    respondHttpStatus(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups")
            .build(),
        HttpStatus.NOT_FOUND);

    userQueryService.findAllGroups();
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_NOK_AccessDenied() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/group1")
            .build(),
        997);
    userQueryService.findAllMembersOfGroup("group1");
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testFindAllMembersOfGroup_NOK_UnknownError() throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/groups/group1")
            .build(),
        999);
    userQueryService.findAllMembersOfGroup("group1");
  }
}
