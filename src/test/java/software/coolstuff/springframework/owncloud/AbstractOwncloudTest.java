package software.coolstuff.springframework.owncloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import software.coolstuff.springframework.owncloud.config.OwncloudResourceFileTestExecutionListener;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudAutoConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OwncloudAutoConfiguration.class })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudResourceFileTestExecutionListener.class
})
public abstract class AbstractOwncloudTest {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

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

  protected void checkAuthorities(Collection<? extends GrantedAuthority> actual, String... expected) {
    Assert.assertEquals(expected.length, actual == null ? 0 : actual.size());
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (ArrayUtils.isNotEmpty(expected)) {
      for (String authority : expected) {
        authorities.add(new SimpleGrantedAuthority(authority));
      }
    }
    if (grantedAuthoritiesMapper != null) {
      Assert.assertTrue(CollectionUtils.isEqualCollection(actual, grantedAuthoritiesMapper.mapAuthorities(authorities)));
    } else {
      Assert.assertTrue(CollectionUtils.isEqualCollection(actual, authorities));
    }
  }

}
