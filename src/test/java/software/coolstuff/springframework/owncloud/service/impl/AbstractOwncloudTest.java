package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.RequestMatcher;

import lombok.Builder;
import lombok.Data;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public abstract class AbstractOwncloudTest {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  protected abstract String getResourcePrefix();

  protected String getResponseContentOf(String testCase) throws IOException {
    String path = "/";
    if (StringUtils.isNotBlank(getResourcePrefix())) {
      if (StringUtils.startsWith(getResourcePrefix(), "/")) {
        path = StringUtils.appendIfMissing(getResourcePrefix(), "/");
      } else {
        path += StringUtils.appendIfMissing(getResourcePrefix(), "/");
      }
    }

    Resource resource = resourceLoader.getResource("classpath:" + path + testCase + ".xml");
    return IOUtils.toString(resource.getInputStream());
  }

  protected RequestMatcher requestToWithPrefix(String uri) {
    String rootURI = properties.getUrl().toString();
    if (StringUtils.isBlank(properties.getUrl().getPath()) || "/".equals(properties.getUrl().getPath())) {
      rootURI = URI.create(properties.getUrl().toString() + AbstractOwncloudServiceImpl.DEFAULT_PATH).toString();
    }

    return requestTo(rootURI + uri);
  }

  protected void checkAuthorities(Collection<? extends GrantedAuthority> actual, String... expected) {
    Assert.assertEquals(expected.length, actual.size());
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (String authority : expected) {
      if (StringUtils.isBlank(properties.getRolePrefix()) || StringUtils.startsWith(authority, properties.getRolePrefix())) {
        authorities.add(new SimpleGrantedAuthority(authority));
      } else {
        authorities.add(new SimpleGrantedAuthority(properties.getRolePrefix() + authority));
      }
    }
    Assert.assertTrue(CollectionUtils.isEqualCollection(actual, authorities));
  }

  protected String getDefaultBasicAuthorizationHeader() {
    return "Basic " + Base64.getEncoder().encodeToString((properties.getUsername() + ":" + properties.getPassword()).getBytes());
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
