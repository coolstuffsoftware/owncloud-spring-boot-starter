package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.net.MalformedURLException;

import org.junit.Test;

public class OwncloudRestServiceIinitializerTest {

  @Test(expected = MalformedURLException.class)
  public void testInvalidURL() throws Exception {
    OwncloudRestUtils.checkAndConvertLocation("totally wrong URL");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWrongProtocol() throws Exception {
    OwncloudRestUtils.checkAndConvertLocation("ftp://illegal.protocol.com");
  }

}
