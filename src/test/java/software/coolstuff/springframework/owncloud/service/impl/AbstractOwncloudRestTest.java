package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.AbstractOwncloudTest;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;

public abstract class AbstractOwncloudRestTest extends AbstractOwncloudTest {

  @Autowired
  private OwncloudProperties properties;

  protected MockRestServiceServer server;

  @Before
  public void setUp() {
    server = createServer(owncloudService());
  }

  protected MockRestServiceServer createServer(AbstractOwncloudServiceImpl owncloudService) {
    return MockRestServiceServer.createServer(owncloudService.getRestTemplate());
  }

  protected abstract AbstractOwncloudServiceImpl owncloudService();

  protected RequestMatcher requestToWithPrefix(String uri) throws MalformedURLException {
    String rootURI = null;
    if (OwncloudResourceService.isNoResource(properties.getLocation())) {
      URL url = new URL(properties.getLocation());
      rootURI = properties.getLocation();
      if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
        rootURI = URI.create(url.toString() + AbstractOwncloudServiceImpl.DEFAULT_PATH).toString();
      }
    }
    return requestTo(rootURI + uri);
  }

  protected String getDefaultBasicAuthorizationHeader() {
    return "Basic " + Base64.getEncoder().encodeToString((properties.getUsername() + ":" + properties.getPassword()).getBytes());
  }

  protected String getSecurityContextBasicAuthorizationHeader() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return "Basic " + Base64.getEncoder().encodeToString((authentication.getName() + ":" + authentication.getCredentials()).getBytes());
  }

  @Data
  @Builder
  public static class Credentials {
    private final String username;
    private final String password;

    public String getForBasicAuthorizationHeader() {
      return "Basic " + Base64.getEncoder().encodeToString((getUsername() + ":" + getPassword()).getBytes());
    }
  }

}
