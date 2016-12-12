package software.coolstuff.springframework.owncloud.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@Configuration
@Profile({ "AUTHORITY-APPENDER-RESOURCE", "AUTHORITY-APPENDER-URL" })
public class AuthorityAppenderConfiguration {

  @Bean
  public OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper() {
    return (username, grantedAuthorities) -> {
      Set<GrantedAuthority> authorities = new HashSet<>(grantedAuthorities);
      authorities.add(new SimpleGrantedAuthority("group98"));
      authorities.add(new SimpleGrantedAuthority("group99"));
      return authorities;
    };
  }

  @Bean
  public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    return (grantedAuthorities) -> {
      throw new IllegalStateException("this Mapper should never been used");
    };
  }

}
