package software.coolstuff.springframework.owncloud.service.api;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public interface OwncloudUserModificationService {

  OwncloudUserDetails saveUser(OwncloudModificationUser userDetails);

  void deleteUser(String username);

  void createGroup(String groupname);

  void deleteGroup(String groupname);

}
