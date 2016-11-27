package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class OwncloudUserDetailsService extends AbstractOwncloudServiceImpl implements UserDetailsService {

  public OwncloudUserDetailsService(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Validate.notBlank(username);

    OcsUserInformation ocsUserInformation = getForObject("/cloud/users/{user}", OcsUserInformation.class, username);
    return loadPreloadedUserByUsername(username, ocsUserInformation);
  }

  public OwncloudUserDetails loadPreloadedUserByUsername(String username, OcsUserInformation preloadedInformation) throws UsernameNotFoundException {
    OcsGroups ocsGroups = getForObject("/cloud/users/{user}/groups", OcsGroups.class, username);
    return createUserDetails(username, preloadedInformation, ocsGroups);
  }

}
