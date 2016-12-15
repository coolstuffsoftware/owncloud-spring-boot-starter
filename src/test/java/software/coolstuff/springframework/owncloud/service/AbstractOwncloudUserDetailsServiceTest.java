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
package software.coolstuff.springframework.owncloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudUserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Test
  public void testCorrectClass() {
    Assert.assertEquals(OwncloudUserDetailsService.class, userDetailsService.getClass());
  }

  @Test
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testUserDetails_OK() throws Exception {
    prepareTestUserDetails_OK("user1", true, "user1@example.com", "Mr. User 1", "group1", "group2");

    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");
    verifyServer();

    Assert.assertNotNull(userDetails);

    Assert.assertEquals("user1", userDetails.getUsername());
    Assert.assertNull(userDetails.getPassword());
    checkAuthorities(userDetails.getUsername(), userDetails.getAuthorities(), "group1", "group2");

    Assert.assertTrue(OwncloudUserDetails.class.isAssignableFrom(userDetails.getClass()));
    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) userDetails;
    Assert.assertEquals("Mr. User 1", owncloudUserDetails.getDisplayName());
    Assert.assertEquals("user1@example.com", owncloudUserDetails.getEmail());
  }

  protected void prepareTestUserDetails_OK(String user, boolean enabled, String email, String displayName,
      String... groups) throws Exception {}

  @Test(expected = UsernameNotFoundException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testUserDetails_NotFound() throws Exception {
    prepareTestUserDetails_NotFound();

    userDetailsService.loadUserByUsername("unknown");
  }

  protected void prepareTestUserDetails_NotFound() throws Exception {}

}
