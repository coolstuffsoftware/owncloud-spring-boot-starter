package software.coolstuff.springframework.owncloud.service.api;

import java.util.List;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

public interface OwncloudUserQueryService {

  List<String> findAllUsers();

  List<String> findAllUsers(String filter);

  List<String> findAllGroups();

  List<String> findAllGroups(String filter);

  List<String> findAllMembersOfGroup(String group);

  OwncloudUserDetails findOneUser(String username);

}
