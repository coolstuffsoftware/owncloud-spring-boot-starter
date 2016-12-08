package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserModificationServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

public abstract class AbstractOwncloudUserModificationServiceRestTest extends AbstractOwncloudUserModificationServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserModificationServiceImpl userModificationService;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  public final AbstractOwncloudServiceImpl owncloudService() {
    return userModificationService;
  }

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithoutGroups(OwncloudModificationUser newUser) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser3_NotFound"), MediaType.TEXT_XML));

    MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
    postData.put("userid", Lists.newArrayList(newUser.getUsername()));
    postData.put("password", Lists.newArrayList(newUser.getPassword()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users"))
        .andExpect(method(POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(postData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser3_AfterCreation"), MediaType.TEXT_XML));

    // change the Displayname
    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(newUser.getDisplayName()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    // change the eMail
    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(newUser.getEmail()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser3_AfterSaveGroups"), MediaType.TEXT_XML));

    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);
    queryServer
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser3_AfterSave"), MediaType.TEXT_XML));
    queryServer
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser3_AfterSaveGroups"), MediaType.TEXT_XML));
  };

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithGroups(OwncloudModificationUser newUser) throws Exception {
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser4_NotFound"), MediaType.TEXT_XML));

    MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
    postData.put("userid", Lists.newArrayList(newUser.getUsername()));
    postData.put("password", Lists.newArrayList(newUser.getPassword()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users"))
        .andExpect(method(POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(postData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser4_AfterCreation"), MediaType.TEXT_XML));

    // change the Displayname
    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(newUser.getDisplayName()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    // change the eMail
    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(newUser.getEmail()));
    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    getServer()
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser4_BeforeGroups"), MediaType.TEXT_XML));

    for (String group : newUser.getGroups()) {
      postData = new LinkedMultiValueMap<>();
      postData.put("groupid", Lists.newArrayList(group));
      getServer()
          .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername() + "/groups"))
          .andExpect(method(POST))
          .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
          .andExpect(content().formData(postData))
          .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));
    }

    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);
    queryServer
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser4_AfterSave"), MediaType.TEXT_XML));
    queryServer
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername() + "/groups"))
        .andExpect(method(GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findUser4_AfterSaveGroups"), MediaType.TEXT_XML));
  }

}
