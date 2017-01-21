package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.net.MalformedURLException;

import org.junit.Test;

import software.coolstuff.springframework.owncloud.service.impl.rest.AbstractOwncloudRestServiceImpl;

public class OwncloudRestServiceIinitializerTest {

  @Test(expected = MalformedURLException.class)
  public void testInvalidURL() throws Exception {
    AbstractOwncloudRestServiceImpl restService = new AbstractOwncloudRestServiceImpl(null) {
      @Override
      public void afterPropertiesSet() throws Exception {
        checkAndConvertLocation("totally wrong URL");
      }
    };
    restService.afterPropertiesSet();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWrongProtocol() throws Exception {
    AbstractOwncloudRestServiceImpl restService = new AbstractOwncloudRestServiceImpl(null) {
      @Override
      public void afterPropertiesSet() throws Exception {
        checkAndConvertLocation("ftp://illegal.protocol.com");
      }
    };
    restService.afterPropertiesSet();
  }

}
