package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Invalid Authentication Object")
public class OwncloudInvalidAuthenticationObjectException extends AuthenticationException {

  private static final long serialVersionUID = -3421422373807935754L;

  public OwncloudInvalidAuthenticationObjectException(Authentication authentication) {
    super("Authentication is not of type " + OwncloudAuthentication.class.getSimpleName() + ". Instead it is of Type " + (authentication == null ? "null" : authentication.getClass().getSimpleName()));
  }

}
