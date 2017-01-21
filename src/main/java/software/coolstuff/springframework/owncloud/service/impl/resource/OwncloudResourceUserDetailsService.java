package software.coolstuff.springframework.owncloud.service.impl.resource;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
class OwncloudResourceUserDetailsService implements UserDetailsService {

  private final OwncloudResourceService resourceService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Get Information about User {} from the Resource Service", username);
    OwncloudResourceData.User user = Optional
        .ofNullable(resourceService.getUser(username))
        .orElseThrow(() -> {
          log.error("User {} not found", username);
          return new UsernameNotFoundException(username);
        });
    return resourceService.convert(user);
  }

}
