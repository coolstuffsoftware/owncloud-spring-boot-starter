package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

@RequiredArgsConstructor
class OwncloudResourceUserDetailsService implements UserDetailsService {

  private final OwncloudResourceService resourceService;

  @Autowired
  private OwncloudUserDetailsConversionService conversionService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    OwncloudResourceData.User user = resourceService.getUser(username);
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
    return conversionService.convert(user);
  }

}
