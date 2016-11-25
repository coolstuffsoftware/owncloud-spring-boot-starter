package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OwncloudUnauthorizedException extends AuthenticationException {

  private static final long serialVersionUID = 4569818474313762346L;

  public OwncloudUnauthorizedException(String msg) {
    super(msg);
  }

  public OwncloudUnauthorizedException(String msg, Throwable t) {
    super(msg, t);
  }

}
