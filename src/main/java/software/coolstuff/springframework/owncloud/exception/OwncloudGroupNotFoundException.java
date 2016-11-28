package software.coolstuff.springframework.owncloud.exception;

public class OwncloudGroupNotFoundException extends OwncloudStatusException {

  private static final long serialVersionUID = -7451968497723587443L;

  public OwncloudGroupNotFoundException(String groupname) {
    super("Group " + groupname + " not found");
  }

}
