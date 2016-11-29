package software.coolstuff.springframework.owncloud.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public class WithMockOwncloudUserFactory implements WithSecurityContextFactory<WithMockOwncloudUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockOwncloudUser withMockOwncloudUser) {
    String username = StringUtils.isNotBlank(withMockOwncloudUser.username()) ? withMockOwncloudUser.username() : withMockOwncloudUser.value();
    Validate.notBlank(username);

    OwncloudUserDetails owncloudUserDetails = OwncloudUserDetails.builder()
        .username(username)
        .password(withMockOwncloudUser.password())
        .build();

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new OwncloudAuthentication(owncloudUserDetails));
    return context;
  }

}
