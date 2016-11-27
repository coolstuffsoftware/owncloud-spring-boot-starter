package software.coolstuff.springframework.owncloud.exception;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

public class OwncloudInvalidAuthentication extends AuthenticationException {

  private static final long serialVersionUID = -3421422373807935754L;

  public OwncloudInvalidAuthentication(Authentication authentication) {
    super("Authentication is not of type " + OwncloudAuthentication.class.getSimpleName() + ". Instead it is of Type " + authentication.getClass().getSimpleName());
  }

}
