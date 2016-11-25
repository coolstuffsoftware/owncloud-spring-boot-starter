package software.coolstuff.springframework.owncloud.model;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = "username")
@ToString(exclude = "password")
public class OwncloudUserDetails implements UserDetails {

  private static final long serialVersionUID = 7384295040126418671L;

  private String username;
  private String password;

  private boolean enabled = true;
  private boolean accountNonExpired = true;
  private boolean accountNonLocked = true;
  private boolean credentialsNonExpired = true;

  private Collection<? extends GrantedAuthority> authorities;

  private String displayName;
  private String email;

}
