package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractOwncloudLocalUserAndGroupServiceImpl {
  @Getter
  private final OwncloudLocalUserDataService localUserDataService;

  protected OwncloudLocalUserData.User getCheckedUser(String username) {
    Validate.notBlank(username);
    log.debug("Get User {} from Resource Service", username);
    OwncloudLocalUserData.User user = localUserDataService.getUser(username);
    OwncloudLocalUtils.validateUserNotNull(user, username);
    return user;
  }
}
