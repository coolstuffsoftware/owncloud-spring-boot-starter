package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService;

interface OwncloudRestUserDetailsService extends OwncloudUserDetailsService, OwncloudRestService {
  OwncloudUserDetails loadPreloadedUserByUsername(String username, Ocs.User preloadedUser) throws UsernameNotFoundException;
}
