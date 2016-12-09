package software.coolstuff.springframework.owncloud.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

@RestClientTest(OwncloudUserDetailsService.class)
public abstract class AbstractOwncloudUserDetailsServiceWithAuthorityAppenderTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserDetailsService userDetailsService;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void testGrantedAuthorityAppenderLoadedByApplicationContext() throws Exception {
    applicationContext.getBean(OwncloudGrantedAuthoritiesMapper.class);
  }

  @Test
  @WithOwncloudMockUser(username = "test1", password = "password")
  public void testAppendedGroups() throws Exception {
    prepareTestAppendedGroups("user1", true, "user1@example.com", "Mr. User 1", "group1", "group2");

    UserDetails userDetails = userDetailsService.loadUserByUsername("user1");
    verifyServer();

    Assert.assertNotNull(userDetails);
    Assert.assertEquals("user1", userDetails.getUsername());

    checkAuthorities(userDetails.getAuthorities(), "group1", "group2", "group98", "group99");
  }

  protected void prepareTestAppendedGroups(String username, boolean enable, String email, String displayName, String... groups) throws Exception {}

}
