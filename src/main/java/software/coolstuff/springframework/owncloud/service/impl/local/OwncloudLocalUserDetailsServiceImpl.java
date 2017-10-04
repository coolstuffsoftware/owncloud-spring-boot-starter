package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class OwncloudLocalUserDetailsServiceImpl implements OwncloudUserDetailsService {

  private final OwncloudLocalUserDataService localDataService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Get Information about User {} from the Resource Service", username);
    OwncloudLocalUserData.User user = Optional
        .ofNullable(localDataService.getUser(username))
        .orElseThrow(() -> {
          log.error("User {} not found", username);
          return new UsernameNotFoundException(username);
        });
    return localDataService.convert(user, true);
  }

}
