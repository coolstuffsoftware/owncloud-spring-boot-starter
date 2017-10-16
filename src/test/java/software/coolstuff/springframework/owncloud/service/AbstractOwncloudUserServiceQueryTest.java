/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
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

    Optional<OwncloudUserDetails> actualUser = userService.findOne("user5");
    verifyServer();

    assertThat(actualUser)
        .isNotNull()
        .isPresent();
    assertThat(actualUser.get().getUsername()).isEqualTo(expectedUser.getUsername());
    assertThat(actualUser.get().getPassword()).isNull();
    assertThat(actualUser.get().getDisplayname()).isEqualTo(expectedUser.getDisplayname());
    assertThat(actualUser.get().getEmail()).isEqualTo(expectedUser.getEmail());
    assertThat(actualUser.get().getQuota()).isEqualByComparingTo(expectedUser.getQuota());

    checkAuthorities(actualUser.get().getUsername(), actualUser.get().getAuthorities(), "group1", "group2");
  }

  protected void prepareTestFindOneUser_OK(OwncloudUserDetails expectedUser, String... groups) throws Exception {}

  @Test
  @WithMockUser(username = "user3", password = "password")
  public void testFindOneUser_UnknownUser() throws Exception {
    prepareTestFindOneUser_UnknownUser("user3");
    Optional<OwncloudUserDetails> emptyUser = userService.findOne("user3");
    assertThat(emptyUser).isNotPresent();
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
