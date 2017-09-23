package software.coolstuff.springframework.owncloud.service.impl.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriUtils;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserServiceQueryTest;

@ActiveProfiles("REST-USER-SERVICE")
public class OwncloudRestUserServiceQueryTest extends AbstractOwncloudUserServiceQueryTest implements OwncloudRestServiceTest {
  @Autowired
  private OwncloudRestUserServiceExtension userService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) userService;
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
  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + expectedUser.getUsername())
            .build(),
        UserResponse.builder()
            .enabled(expectedUser.isEnabled())
            .email(expectedUser.getEmail())
            .displayname(expectedUser.getDisplayname())
            .quota(expectedUser.getQuota())
            .build());
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

  @Test
  @WithMockUser(username = "user1", password = "password")
  public void test_getQuota_OK() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    OwncloudQuota expected = TestOwncloudQuota.builder()
        .username(username)
        .total(1024l)
        .used(1024l)
        .free(0l)
        .relative(100.0f)
        .build();
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + username)
            .build(),
        UserResponse.builder()
            .quota(expected.getTotal())
            .used(expected.getUsed())
            .free(expected.getFree())
            .relative(expected.getRelative())
            .build());

    OwncloudRestQuotaImpl quota = userService.getQuota(username);
    assertThat(quota)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(expected, "username", "total", "used", "free", "relative");
  }

  @Data
  @Builder
  private static class TestOwncloudQuota implements OwncloudQuota {
    private final String username;
    private final long total;
    private final long used;
    private final long free;
    private final float relative;
  }
}
