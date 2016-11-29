package software.coolstuff.springframework.owncloud.service.api;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public interface OwncloudUserModificationService {

  void saveUser(OwncloudUserDetails userDetails);

}
