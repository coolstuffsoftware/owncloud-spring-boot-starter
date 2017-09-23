package software.coolstuff.springframework.owncloud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserService.class)
public abstract class AbstractOwncloudUserServiceQueryTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserService userService;

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllUsers() throws Exception {
    prepareTestFindAllUsers("user1", "user2", "user5");

    List<String> users = userService.findAll();
    verifyServer();

    assertThat(users)
        .isNotNull()
        .containsOnly("user1", "user2", "user5");
  }

  protected void prepareTestFindAllUsers(String... users) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindAllUsersWithFilter() throws Exception {
    prepareTestFindAllUsersWithFilter("User 1", "user1");

    List<String> users = userService.findAll("User 1");
    verifyServer();

    assertThat(users)
        .isNotNull()
        .containsOnly("user1");
  }

  protected void prepareTestFindAllUsersWithFilter(String filter, String... users) throws Exception {}

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_OK() throws Exception {
    OwncloudUserDetails expectedUser = OwncloudUserDetails.builder()
        .username("user5")
        .enabled(true)
        .email("user5@example.com")
        .displayname("Mr. User 5")
        .quota(1024L)
        .build();
    prepareTestFindOneUser_OK(expectedUser, "group1", "group2");

    OwncloudUserDetails actualUser = userService.findOne("user5");
    verifyServer();

    assertThat(actualUser).isNotNull();
    assertThat(actualUser.getUsername()).isEqualTo(expectedUser.getUsername());
    assertThat(actualUser.getPassword()).isNull();
    assertThat(actualUser.getDisplayname()).isEqualTo(expectedUser.getDisplayname());
    assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
    assertThat(actualUser.getQuota()).isEqualByComparingTo(expectedUser.getQuota());

    checkAuthorities(actualUser.getUsername(), actualUser.getAuthorities(), "group1", "group2");
  }

  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithMockUser(username = "user3", password = "password")
  public void testFindOneUser_UnknownUser() throws Exception {
    prepareTestFindOneUser_UnknownUser("user3");
    userService.findOne("user3");
  }

  protected void prepareTestFindOneUser_UnknownUser(String user) throws Exception {}

  @Test(expected = NullPointerException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_NoUser() {
    userService.findOne(null);
  }

  @Test(expected = IllegalArgumentException.class)
  @WithMockUser(username = "user1", password = "password")
  public void testFindOneUser_BlankUser() {
    userService.findOne("");
  }
}
