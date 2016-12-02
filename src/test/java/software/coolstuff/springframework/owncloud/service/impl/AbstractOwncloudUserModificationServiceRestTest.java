package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@RestClientTest(OwncloudUserModificationService.class)
public abstract class AbstractOwncloudUserModificationServiceRestTest extends AbstractOwncloudRestTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  protected final AbstractOwncloudServiceImpl owncloudService() {
    return (OwncloudUserModificationServiceImpl) userModificationService;
  }

  @Override
  protected final String getResourcePrefix() {
    return "/modificationService";
  }

  @Test
  public void testSaveUser_CreateUser_OK() throws MalformedURLException, IOException {
    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);

    OwncloudUserDetails newUser = OwncloudUserDetails.builder()
        .username("user1")
        .password("password")
        .enabled(true)
        .displayName("Mr. User 1")
        .email("user1@example.com")
        .build();

    server
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_UnknownUser"), MediaType.TEXT_XML));

    MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
    postData.put("userid", Lists.newArrayList(newUser.getUsername()));
    postData.put("password", Lists.newArrayList(newUser.getPassword()));
    server
        .expect(requestToWithPrefix("/cloud/users"))
        .andExpect(method(POST))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andExpect(content().formData(postData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));
    server
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("findOneUser_AfterCreation"), MediaType.TEXT_XML));

    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(newUser.getDisplayName()));
    server
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(newUser.getEmail()));
    server
        .expect(requestToWithPrefix("/cloud/users/" + newUser.getUsername()))
        .andExpect(method(PUT))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andExpect(content().formData(putData))
        .andRespond(withSuccess(getResponseContentOf("success"), MediaType.TEXT_XML));

    newUser = userModificationService.saveUser(newUser);
  }

  protected abstract String getBasicAuthorizationHeader();

}
