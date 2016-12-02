package software.coolstuff.springframework.owncloud.service.impl.rest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.config.WithMockOwncloudUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudRestTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudUserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceRestTest extends AbstractOwncloudRestTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Override
  protected final String getResourcePrefix() {
    return "/userDetails";
  }

  @Override
  protected final OwncloudUserDetailsService owncloudService() {
    return userDetailsService;
  }

  @Test
  public void testCorrectClass() {
    Assert.assertEquals(OwncloudUserDetailsService.class, userDetailsService.getClass());
  }

  @Test
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testUserDetails_OK() throws IOException {
    server.expect(requestToWithPrefix("/cloud/users/user1")).andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_details"), MediaType.TEXT_XML));
    server.expect(requestToWithPrefix("/cloud/users/user1/groups")).andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("user1_groups"), MediaType.TEXT_XML));

    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");
    server.verify();

    Assert.assertNotNull(userDetails);

    Assert.assertEquals("user1", userDetails.getUsername());
    Assert.assertNull(userDetails.getPassword());
    checkAuthorities(userDetails.getAuthorities(), "Group1", "Group2");

    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(userDetails.getClass()));
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetails;
    Assert.assertEquals("Mr. User 1", owncloudUserDetails.getDisplayName());
    Assert.assertEquals("user1@example.com", owncloudUserDetails.getEmail());
  }

  protected abstract String getBasicAuthorizationHeader();

  @Test(expected = UsernameNotFoundException.class)
  @WithMockOwncloudUser(username = "user1", password = "password")
  public void testUserDetials_NotFound() throws IOException {
    server.expect(requestToWithPrefix("/cloud/users/unknown")).andExpect(method(GET))
        .andExpect(header("Authorization", getBasicAuthorizationHeader()))
        .andRespond(withSuccess(getResponseContentOf("unknown_user"), MediaType.TEXT_XML));

    userDetailsService.loadUserByUsername("unknown");
  }

}
