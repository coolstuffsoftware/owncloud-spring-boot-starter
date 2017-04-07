package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

@RequiredArgsConstructor
@Slf4j
class OwncloudLocalUserServiceImpl implements OwncloudLocalUserService {

  private final OwncloudLocalUserDataService localDataService;

  private final List<Consumer<OwncloudUserDetails>> saveUserListeners = new ArrayList<>();
  private final List<Consumer<String>> deleteUserListeners = new ArrayList<>();

  @Override
  public OwncloudUserDetails save(OwncloudModificationUser modificationUser) {
    Validate.notNull(modificationUser);
    Validate.notBlank(modificationUser.getUsername());

    log.debug("Try to get User Information of User {} from the Resource Service", modificationUser.getUsername());
    OwncloudLocalUserData.User existingUser = localDataService.getUser(modificationUser.getUsername());

    if (existingUser == null) {
      Validate.notBlank(modificationUser.getPassword());

      log.debug("User {} not exists --> Create new User", modificationUser.getUsername());
      existingUser = new OwncloudLocalUserData.User();
      existingUser.setUsername(modificationUser.getUsername());
      existingUser.setPassword(modificationUser.getPassword());
      localDataService.addUser(existingUser);
      log.info("User {} successfully created", existingUser.getUsername());
    }

    log.debug("Modify Information of User {}", existingUser.getUsername());
    existingUser.setDisplayname(modificationUser.getDisplayname());
    existingUser.setEmail(modificationUser.getEmail());
    existingUser.setQuota(modificationUser.getQuota());
    existingUser.setEnabled(modificationUser.isEnabled());

    manageGroups(existingUser, modificationUser);

    OwncloudUserDetails changedUserDetails = localDataService.convert(existingUser);
    log.debug("Notify registered Listeners about changed UserDetails {}", changedUserDetails);
    saveUserListeners.stream()
        .forEach(listener -> listener.accept(changedUserDetails));
    log.info("User {} successfully modified", changedUserDetails.getUsername());
    return changedUserDetails;
  }

  private void manageGroups(OwncloudLocalUserData.User existingUser, OwncloudModificationUser newUser) {
    Set<String> groups = new HashSet<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      log.debug("Modify the Group Memberships of User {}", existingUser.getUsername());
      for (String groupname : newUser.getGroups()) {
        if (localDataService.groupNotExists(groupname)) {
          log.error("Group {} doesn't exist. Can't assign the User {} to this non-existing Group", groupname, existingUser.getUsername());
          throw new OwncloudGroupNotFoundException(groupname);
        }
        log.trace("Assign Group {} to User {}", groupname, existingUser.getUsername());
        groups.add(groupname);
      }
    }
    existingUser.setGroups(groups);
  }

  @Override
  public void delete(String username) {
    Validate.notBlank(username);
    if (localDataService.userNotExists(username)) {
      log.error("User {} doesn't exist", username);
      throw new UsernameNotFoundException(username);
    }
    log.debug("Remove User {}", username);
    localDataService.removeUser(username);
    log.debug("Notify registered Listeners about removed User {}", username);
    deleteUserListeners.stream()
        .forEach(listener -> listener.accept(username));
    log.info("User {} successfully removed", username);
  }

  @Override
  public void registerSaveUserCallback(Consumer<OwncloudUserDetails> listener) {
    saveUserListeners.add(listener);
  }

  @Override
  public void registerDeleteUserCallback(Consumer<String> listener) {
    deleteUserListeners.add(listener);
  }

}
