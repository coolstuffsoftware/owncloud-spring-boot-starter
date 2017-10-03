package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.CheckOwncloudModification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class OwncloudLocalUserServiceImpl extends AbstractOwncloudLocalUserAndGroupServiceImpl implements OwncloudLocalUserServiceExtension {

  private final List<Consumer<OwncloudUserDetails>> saveUserListeners = new ArrayList<>();
  private final List<Consumer<String>> deleteUserListeners = new ArrayList<>();

  public OwncloudLocalUserServiceImpl(OwncloudLocalUserDataService localUserDataService) {
    super(localUserDataService);
  }

  @Override
  public List<String> findAll() {
    return findAll(null);
  }

  @Override
  public List<String> findAll(String filter) {
    log.debug("Get all Users with a DisplayName like {}", filter);
    List<String> filteredUsers = new ArrayList<>();
    for (OwncloudLocalUserData.User user : getLocalUserDataService().getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayname(), filter)) {
        log.trace("add User {} to the Result", user.getUsername());
        filteredUsers.add(user.getUsername());
      }
    }
    return filteredUsers;
  }

  @Override
  public OwncloudUserDetails findOne(String username) {
    OwncloudLocalUserData.User user = getCheckedUser(username);
    return getLocalUserDataService().convert(user, false);
  }

  @Override
  @CheckOwncloudModification
  public OwncloudUserDetails save(OwncloudModificationUser modificationUser) {
    Validate.notNull(modificationUser);
    Validate.notBlank(modificationUser.getUsername());

    log.debug("Try to get User Information of User {} from the Resource Service", modificationUser.getUsername());
    OwncloudLocalUserData.User existingUser = getLocalUserDataService().getUser(modificationUser.getUsername());

    if (existingUser == null) {
      Validate.notBlank(modificationUser.getPassword());

      log.debug("User {} not exists --> Create new User", modificationUser.getUsername());
      existingUser = new OwncloudLocalUserData.User();
      existingUser.setUsername(modificationUser.getUsername());
      existingUser.setPassword(modificationUser.getPassword());
      getLocalUserDataService().addUser(existingUser);
      log.info("User {} successfully created", existingUser.getUsername());
    }

    log.debug("Modify Information of User {}", existingUser.getUsername());
    existingUser.setDisplayname(modificationUser.getDisplayname());
    existingUser.setEmail(modificationUser.getEmail());
    existingUser.setQuota(modificationUser.getQuota());
    existingUser.setEnabled(modificationUser.isEnabled());

    manageGroups(existingUser, modificationUser);

    OwncloudUserDetails changedUserDetails = getLocalUserDataService().convert(existingUser, false);
    log.debug("Notify registered Listeners about changed UserDetails {}", changedUserDetails);
    saveUserListeners.forEach(listener -> listener.accept(changedUserDetails));
    log.info("User {} successfully modified", changedUserDetails.getUsername());
    return changedUserDetails;
  }

  private void manageGroups(OwncloudLocalUserData.User existingUser, OwncloudModificationUser newUser) {
    Set<String> groups = new HashSet<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      log.debug("Modify the Group Memberships of User {}", existingUser.getUsername());
      for (String groupname : newUser.getGroups()) {
        if (getLocalUserDataService().groupNotExists(groupname)) {
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
  @CheckOwncloudModification
  public void delete(String username) {
    Validate.notBlank(username);
    if (getLocalUserDataService().userNotExists(username)) {
      log.error("User {} doesn't exist", username);
      throw new UsernameNotFoundException(username);
    }
    log.debug("Remove User {}", username);
    getLocalUserDataService().removeUser(username);
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
