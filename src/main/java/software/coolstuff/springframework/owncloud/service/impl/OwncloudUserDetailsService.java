package software.coolstuff.springframework.owncloud.service.impl;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class OwncloudUserDetailsService extends AbstractOwncloudServiceImpl implements UserDetailsService {

  public OwncloudUserDetailsService(RestTemplateBuilder builder, OwncloudProperties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    super(builder, properties, true, messageConverter);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Validate.notBlank(username);

    OcsUserInformation ocsUserInformation = getForObject("/users/{user}", OcsUserInformation.class, username);
    OcsGroups ocsGroups = getForObject("/users/{user}/groups", OcsGroups.class, username);
    return createUserDetails(username, ocsUserInformation, ocsGroups);
  }

}
