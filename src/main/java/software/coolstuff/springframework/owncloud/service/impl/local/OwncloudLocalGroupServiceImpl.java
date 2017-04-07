package software.coolstuff.springframework.owncloud.service.impl.local;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;

@RequiredArgsConstructor
@Slf4j
class OwncloudLocalGroupServiceImpl implements OwncloudGroupService {

  private final OwncloudLocalUserDataService localDataService;

  @Override
  public void create(String groupname) {
    Validate.notBlank(groupname);
    if (localDataService.groupExists(groupname)) {
      log.error("Group {} already exists", groupname);
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    log.debug("Create Group {}", groupname);
    localDataService.addGroup(groupname);
    log.info("Group {} successfully created");
  }

  @Override
  public void delete(String groupname) {
    Validate.notBlank(groupname);

    log.debug("Get Information of Group {} from the Resource Service", groupname);
    String group = localDataService.getGroup(groupname);
    if (StringUtils.isBlank(group)) {
      throw new OwncloudGroupNotFoundException(groupname);
    }

    for (OwncloudLocalUserData.User user : localDataService.getUsers()) {
      if (user.getGroups() != null) {
        log.trace("Revoke Assignment of Group {} from User {}", groupname, user.getUsername());
        user.getGroups().remove(group);
      }
    }
    log.debug("Remove Group {}", groupname);
    localDataService.removeGroup(groupname);
    log.info("Group {} successfully removed", groupname);
  }

}
