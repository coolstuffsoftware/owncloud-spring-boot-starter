package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@RequiredArgsConstructor
@Slf4j
public class OwncloudLocalUserQueryServiceImpl implements OwncloudUserQueryService {

  private final OwncloudLocalUserDataService localDataService;

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    log.debug("Get all Users with a DisplayName like {}", filter);
    List<String> filteredUsers = new ArrayList<>();
    for (OwncloudLocalUserData.User user : localDataService.getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayname(), filter)) {
        log.trace("add User {} to the Result", user.getUsername());
        filteredUsers.add(user.getUsername());
      }
    }
    return filteredUsers;
  }

  private OwncloudLocalUserData.User getCheckedUser(String username) {
    Validate.notBlank(username);
    log.debug("Get User {} from Resource Service", username);
    OwncloudLocalUserData.User user = localDataService.getUser(username);
    OwncloudLocalUtils.validateUserNotNull(user, username);
    return user;
  }

  @Override
  public OwncloudUserDetails findOne(String username) {
    OwncloudLocalUserData.User user = getCheckedUser(username);
    return localDataService.convert(user);
  }

}
