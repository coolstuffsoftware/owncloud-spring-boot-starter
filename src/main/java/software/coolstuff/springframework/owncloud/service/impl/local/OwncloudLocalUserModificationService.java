package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

@RequiredArgsConstructor
@Slf4j
class OwncloudLocalUserModificationService implements OwncloudUserModificationService {

  private final OwncloudLocalDataService localDataService;

  @Override
  public OwncloudUserDetails saveUser(OwncloudModificationUser modificationUser) {
    Validate.notNull(modificationUser);
    Validate.notBlank(modificationUser.getUsername());

    log.debug("Try to get User Information of User {} from the Resource Service", modificationUser.getUsername());
    OwncloudLocalData.User existingUser = localDataService.getUser(modificationUser.getUsername());

    if (existingUser == null) {
      Validate.notBlank(modificationUser.getPassword());

      log.debug("User {} not exists --> Create new User", modificationUser.getUsername());
      existingUser = new OwncloudLocalData.User();
      existingUser.setUsername(modificationUser.getUsername());
      existingUser.setPassword(modificationUser.getPassword());
      localDataService.addUser(existingUser);
      log.info("User {} successfully created", existingUser.getUsername());
    }

    log.debug("Modify Information of User {}", existingUser.getUsername());
    existingUser.setDisplayname(modificationUser.getDisplayname());
    existingUser.setEmail(modificationUser.getEmail());
    existingUser.setEnabled(modificationUser.isEnabled());

    manageGroups(existingUser, modificationUser);

    OwncloudUserDetails changedUserDetails = localDataService.convert(existingUser);
    log.info("User {} successfully modified", changedUserDetails.getUsername());
    return changedUserDetails;
  }

  private void manageGroups(OwncloudLocalData.User existingUser, OwncloudModificationUser newUser) {
    List<OwncloudLocalData.Group> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      log.debug("Modify the Group Memberships of User {}", existingUser.getUsername());
      for (String groupname : newUser.getGroups()) {
        if (localDataService.groupNotExists(groupname)) {
          log.error("Group {} doesn't exist. Can't assign the User {} to this non-existing Group", groupname, existingUser.getUsername());
          throw new OwncloudGroupNotFoundException(groupname);
        }
        log.trace("Assign Group {} to User {}", groupname, existingUser.getUsername());
        groups.add(new OwncloudLocalData.Group(groupname));
      }
    }
    existingUser.setGroups(groups);
  }

  @Override
  public void deleteUser(String username) {
    Validate.notBlank(username);
    if (localDataService.userNotExists(username)) {
      log.error("User {} doesn't exist", username);
      throw new UsernameNotFoundException(username);
    }
    log.debug("Remove User {}", username);
    localDataService.removeUser(username);
    log.info("User {} successfully removed", username);
  }

  @Override
  public void createGroup(String groupname) {
    Validate.notBlank(groupname);
    if (localDataService.groupExists(groupname)) {
      log.error("Group {} already exists", groupname);
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    log.debug("Create Group {}", groupname);
    localDataService.addGroup(new OwncloudLocalData.Group(groupname));
    log.info("Group {} successfully created");
  }

  @Override
  public void deleteGroup(String groupname) {
    Validate.notBlank(groupname);

    log.debug("Get Information of Group {} from the Resource Service", groupname);
    OwncloudLocalData.Group group = localDataService.getGroup(groupname);
    OwncloudLocalUtils.validateGroupNotNull(group, groupname);

    for (OwncloudLocalData.User user : localDataService.getUsers()) {
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
