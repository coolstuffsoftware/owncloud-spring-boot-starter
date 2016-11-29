package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OwncloudUsernameAlreadyExistsException extends OwncloudStatusException {

  private static final long serialVersionUID = 9039656661419987314L;

  public OwncloudUsernameAlreadyExistsException(String msg) {
    super(msg);
  }
}
