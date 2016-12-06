package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

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
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllUsers"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllGroups() throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllGroups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_OK(String group) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups/" + group))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_OK"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllMembersOfGroup_UnknownGroup(String group) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/groups/" + group))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findAllMembersOfGroup_UnknownGroup"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindOneUser_OK(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Information"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user + "/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_UnknownUser"), MediaType.TEXT_XML));
  }

  @Override
  protected void prepareTestFindAllGroupsOfUser_OK(String user) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + user + "/groups"))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_Groups"), MediaType.TEXT_XML));
  }
}
