package software.coolstuff.springframework.owncloud.service.api;

import java.util.List;

public interface OwncloudUserQueryService {

  List<String> getAllUsers(String filter);

  List<String> getAllGroups(String filter);

  List<String> getAllMembersOfGroup(String group);

}
