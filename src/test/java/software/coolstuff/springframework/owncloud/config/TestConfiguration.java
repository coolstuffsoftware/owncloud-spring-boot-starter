package software.coolstuff.springframework.owncloud.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@SpringBootConfiguration
@Profile({ "AUTHORITY-APPENDER-RESOURCE", "AUTHORITY-APPENDER-URL" })
public class TestConfiguration {

  @Bean
  public OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper() {
    return (username, grantedAuthorities) -> {
      Set<GrantedAuthority> authorities = new HashSet<>(grantedAuthorities);
      authorities.add(new SimpleGrantedAuthority("group98"));
      authorities.add(new SimpleGrantedAuthority("group99"));
      return authorities;
    };
  }

}
