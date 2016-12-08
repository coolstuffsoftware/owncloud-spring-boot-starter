package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class OwncloudUserDetailsService extends AbstractOwncloudServiceImpl implements UserDetailsService {

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  public OwncloudUserDetailsService(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Validate.notBlank(username);

    if (isRestAvailable()) {
      Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
      return loadPreloadedUserByUsername(username, user);
    }

    return loadUserByUsernameFromResourceService(username);
  }

  OwncloudUserDetails loadPreloadedUserByUsername(String username, Ocs.User preloadedUser) throws UsernameNotFoundException {
    Ocs.Groups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return createUserDetails(username, preloadedUser, groups);
  }

  public OwncloudUserDetails loadUserByUsernameFromResourceService(String username) {
    if (resourceService == null) {
      throw new UsernameNotFoundException("resourceService is not available");
    }

    OwncloudUserDetails userDetails = resourceService.getUser(username);
    if (userDetails == null) {
      throw new UsernameNotFoundException(username);
    }

    return userDetails;
  }
}
