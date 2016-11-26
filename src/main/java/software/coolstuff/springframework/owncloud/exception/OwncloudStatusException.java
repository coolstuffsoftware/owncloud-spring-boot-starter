package software.coolstuff.springframework.owncloud.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class OwncloudStatusException extends RuntimeException {

  private static final long serialVersionUID = -3354624827447206574L;

  public OwncloudStatusException(String msg) {
    super(msg);
  }

  public OwncloudStatusException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public OwncloudStatusException(Throwable cause) {
    super(cause);
  }

}
