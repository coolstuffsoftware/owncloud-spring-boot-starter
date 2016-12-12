package software.coolstuff.springframework.owncloud.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

@Configuration
@Profile({ "AUTHORITY-MAPPER-RESOURCE", "AUTHORITY-MAPPER-URL" })
public class AuthorityMapperConfiguration {

  @Bean
  public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
    return (grantedAuthorities) -> {
      if (CollectionUtils.isEmpty(grantedAuthorities)) {
        return grantedAuthorities;
      }

      List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
      for (GrantedAuthority grantedAuthority : grantedAuthorities) {
        mappedAuthorities.add(new SimpleGrantedAuthority("MAPPED_" + grantedAuthority.getAuthority()));
      }
      return mappedAuthorities;
    };
  }

}
