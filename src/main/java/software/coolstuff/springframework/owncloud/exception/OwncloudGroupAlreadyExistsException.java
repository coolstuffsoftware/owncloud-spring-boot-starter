package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OwncloudGroupAlreadyExistsException extends OwncloudStatusException {

  private static final long serialVersionUID = -759133175245748971L;

  public OwncloudGroupAlreadyExistsException(String msg) {
    super(msg);
  }

}
