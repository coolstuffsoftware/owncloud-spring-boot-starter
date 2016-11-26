package software.coolstuff.springframework.owncloud.model;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import lombok.Data;

@Data
public class OwncloudAuthentication implements Authentication {

  private static final long serialVersionUID = -7867915541247069469L;

  @NotNull
  private final OwncloudUserDetails owncloudUserDetails;

  @Override
  public String getName() {
    return owncloudUserDetails.getUsername();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return owncloudUserDetails.getAuthorities();
  }

  @Override
  public Object getCredentials() {
    return owncloudUserDetails.getPassword();
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return owncloudUserDetails;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

}
