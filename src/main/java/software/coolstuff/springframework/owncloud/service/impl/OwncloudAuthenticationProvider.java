package software.coolstuff.springframework.owncloud.service.impl;

import java.util.Base64;

import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import lombok.Getter;
import lombok.Setter;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class OwncloudAuthenticationProvider extends AbstractOwncloudServiceImpl implements AuthenticationProvider {

  @Setter
  @Getter
  private String rolePrefix = "ROLE_";

  public OwncloudAuthenticationProvider(RestTemplateBuilder builder, Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    super(builder, properties, false, messageConverter);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    Validate.notBlank(username);

    String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Basic " + encodedCredentials);
    final HttpEntity<String> request = new HttpEntity<>(headers);

    OcsUserInformation ocsUserInformation = exchange("/users/{user}", HttpMethod.GET, request, OcsUserInformation.class, username);
    OcsGroups ocsUserGroups = exchange("/users/{user}/groups", HttpMethod.GET, request, OcsGroups.class, username);

    OwncloudUserDetails owncloudUserDetails = createUserDetails(username, ocsUserInformation, ocsUserGroups);
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication) ||
        OwncloudAuthentication.class.isAssignableFrom(authentication);
  }

}
