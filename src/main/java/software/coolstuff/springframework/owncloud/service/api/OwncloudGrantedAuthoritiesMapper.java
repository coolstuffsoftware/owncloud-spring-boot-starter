package software.coolstuff.springframework.owncloud.service.api;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

public interface OwncloudGrantedAuthoritiesMapper {

  Collection<? extends GrantedAuthority> mapAuthorities(String username, Collection<? extends GrantedAuthority> grantedAuthorities);

}
