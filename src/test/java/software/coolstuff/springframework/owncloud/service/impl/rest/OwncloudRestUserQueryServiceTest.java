/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
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
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserQueryServiceTest;

@ActiveProfiles("REST-USER-SERVICE")
public class OwncloudRestUserQueryServiceTest extends AbstractOwncloudUserQueryServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudRestUserQueryService userQueryService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) userQueryService;
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

    OwncloudRestQuota quota = userQueryService.getQuota(username);
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
