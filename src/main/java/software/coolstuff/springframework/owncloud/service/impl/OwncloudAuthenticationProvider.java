package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder, Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    super(builder, properties, false, messageConverter);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

}
