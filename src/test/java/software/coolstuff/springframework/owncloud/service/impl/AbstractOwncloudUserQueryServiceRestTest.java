package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;

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
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/users").build();
    respondUsers(request, users);
  }

  @Override
  protected void prepareTestFindAllGroups(String... groups) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/groups").build();
    respondGroups(request, groups);
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_OK(String group, String... users) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/groups/" + group).build();
    respondUsers(request, users);
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/groups/" + group).build();
    respondFailure(request, 998, "The requested group could not be found");
  }

  @Override
  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/users/" + expectedUser.getUsername()).build();
    respondUser(request, expectedUser.isEnabled(), expectedUser.getEmail(), expectedUser.getDisplayName());

    request = RestRequest.builder().method(GET).url("/cloud/users/" + expectedUser.getUsername() + "/groups").build();
    respondGroups(request, groups);
  }

  @Override
  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/users/" + user).build();
    respondFailure(request, 998, "The requested user could not be found");
  }

  @Override
  protected void prepareTestFindAllGroupsOfUser_OK(String user, String... groups) throws Exception {
    RestRequest request = RestRequest.builder().method(GET).url("/cloud/users/" + user + "/groups").build();
    respondGroups(request, groups);
  }

  @Test(expected = BadCredentialsException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testBadCredentialsBy401() throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withUnauthorizedRequest());

    userQueryService.findAllGroups();
  }

  @Test(expected = HttpClientErrorException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testNotFound() throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    userQueryService.findAllGroups();
  }
}
