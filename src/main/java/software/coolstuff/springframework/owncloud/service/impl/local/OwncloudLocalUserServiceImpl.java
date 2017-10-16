/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.util.*;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.CheckOwncloudModification;

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
  public Optional<OwncloudUserDetails> findOne(String username) {
    try {
      OwncloudLocalUserData.User user = getCheckedUser(username);
      return Optional.of(getLocalUserDataService().convert(user, false));
    } catch (UsernameNotFoundException ignored) {
      return Optional.empty();
    }
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
    deleteUserListeners.forEach(listener -> listener.accept(username));
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
