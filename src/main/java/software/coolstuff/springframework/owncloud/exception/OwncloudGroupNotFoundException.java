package software.coolstuff.springframework.owncloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Group not found")
public class OwncloudGroupNotFoundException extends OwncloudStatusException {

  private static final long serialVersionUID = -7451968497723587443L;

  public OwncloudGroupNotFoundException(String groupname) {
    super("Group " + groupname + " not found");
  }

}
