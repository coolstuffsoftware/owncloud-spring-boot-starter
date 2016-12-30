package software.coolstuff.springframework.owncloud.service.impl;

import java.net.MalformedURLException;

import org.junit.Test;

public class OwncloudRestServiceTest {

  @Test(expected = MalformedURLException.class)
  public void testInvalidURL() throws Exception {
    AbstractOwncloudRestServiceImpl restService = new AbstractOwncloudRestServiceImpl(null) {
      @Override
      public void afterPropertiesSet() throws Exception {
        checkLocation("totally wrong URL");
      }
    };
    restService.afterPropertiesSet();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWrongProtocol() throws Exception {
    AbstractOwncloudRestServiceImpl restService = new AbstractOwncloudRestServiceImpl(null) {
      @Override
      public void afterPropertiesSet() throws Exception {
        checkLocation("ftp://illegal.protocol.com");
      }
    };
    restService.afterPropertiesSet();
  }

}
