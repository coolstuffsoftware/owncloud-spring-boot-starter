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
    getServer()
        .expect(requestToWithPrefix("/cloud/users"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllUsers"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllGroups() throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllGroups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_OK(String group) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups/" + group))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_OK"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups/" + group))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_UnknownGroup"), MediaType.TEXT_XML));
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
