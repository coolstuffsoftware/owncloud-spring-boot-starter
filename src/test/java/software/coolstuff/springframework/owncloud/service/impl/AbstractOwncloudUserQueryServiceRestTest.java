package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;

public abstract class AbstractOwncloudUserQueryServiceRestTest extends AbstractOwncloudUserQueryServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserQueryServiceImpl userQueryService;

  @Override
  public final AbstractOwncloudServiceImpl owncloudService() {
    return userQueryService;
  }

  @Override
  protected void prepareTestFindAllUsers() throws Exception {
    RestRequest request = RestRequest.builder()
        .server(getServer())
        .method(GET)
        .url("/cloud/users")
        .basicAuthorization(this::getBasicAuthorizationHeader)
        .build();
    respondUsers(request, "user1", "user2");
  }

  @Override
  protected void prepareTestFindAllGroups() throws Exception {
    RestRequest request = RestRequest.builder()
        .server(getServer())
        .method(GET)
        .url("/cloud/groups")
        .basicAuthorization(this::getBasicAuthorizationHeader)
        .build();
    respondGroups(request, "group1", "group2");
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_OK(String group) throws Exception {
    RestRequest request = RestRequest.builder()
        .server(getServer())
        .method(GET)
        .url("/cloud/groups/" + group)
        .basicAuthorization(this::getBasicAuthorizationHeader)
        .build();
    respondUsers(request, "user1");
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {
    RestRequest request = RestRequest.builder()
        .server(getServer())
        .method(GET)
        .url("/cloud/groups/" + group)
        .basicAuthorization(this::getBasicAuthorizationHeader)
        .build();
    respondFailure(request, 998, "The requested group could not be found");
  }

  @Override
  protected void prepareTestFindOneUser_OK(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Information"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_UnknownUser"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllGroupsOfUser_OK(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));
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
