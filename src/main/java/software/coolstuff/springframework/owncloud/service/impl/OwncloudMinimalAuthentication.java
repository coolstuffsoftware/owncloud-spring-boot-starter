package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import lombok.Data;

@Data
class OwncloudMinimalAuthentication implements Authentication {

  private static final long serialVersionUID = -8435147516189570199L;

  private final String username;
  private final String password;

  @Override
  public String getName() {
    return username;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return new ArrayList<>();
  }

  @Override
  public Object getCredentials() {
    return password;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }

  @Override
  public boolean isAuthenticated() {
    return false;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

}
