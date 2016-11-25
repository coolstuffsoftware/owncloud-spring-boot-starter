package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class OwncloudUserDetailsService extends AbstractOwncloudServiceImpl implements UserDetailsService {

  public OwncloudUserDetailsService(RestTemplateBuilder builder, Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    super(builder, properties, true, messageConverter);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

}
