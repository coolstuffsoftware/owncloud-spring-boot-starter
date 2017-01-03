package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.Group;

@RequiredArgsConstructor
@Slf4j
class OwncloudUserModificationResourceService implements OwncloudUserModificationService {

  private final OwncloudResourceService resourceService;

  @Autowired
  private OwncloudUserDetailsConversionService conversionService;

  @Override
  public OwncloudUserDetails saveUser(OwncloudModificationUser modificationUser) {
    Validate.notNull(modificationUser);
    Validate.notBlank(modificationUser.getUsername());

    log.debug("Try to get User Information of User {} from the Resource Service", modificationUser.getUsername());
    OwncloudResourceData.User existingUser = resourceService.getUser(modificationUser.getUsername());

    if (existingUser == null) {
      Validate.notBlank(modificationUser.getPassword());

      log.debug("User {} not exists --> Create new User", modificationUser.getUsername());
      existingUser = new OwncloudResourceData.User();
      existingUser.setUsername(modificationUser.getUsername());
      existingUser.setPassword(modificationUser.getPassword());
      resourceService.addUser(existingUser);
      log.info("User {} successfully created", existingUser.getUsername());
    }

    log.debug("Modify Information of User {}", existingUser.getUsername());
    existingUser.setDisplayName(modificationUser.getDisplayName());
    existingUser.setEmail(modificationUser.getEmail());
    existingUser.setEnabled(modificationUser.isEnabled());

    manageGroups(existingUser, modificationUser);

    OwncloudUserDetails changedUserDetails = conversionService.convert(existingUser);
    log.info("User {} successfully modified", changedUserDetails.getUsername());
    return changedUserDetails;
  }

  private void manageGroups(OwncloudResourceData.User existingUser, OwncloudModificationUser newUser) {
    List<OwncloudResourceData.Group> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      log.debug("Modify the Group Memberships of User {}", existingUser.getUsername());
      for (String groupname : newUser.getGroups()) {
        if (resourceService.groupNotExists(groupname)) {
          log.error("Group {} doesn't exist. Can't assign the User {} to this non-existing Group", groupname, existingUser.getUsername());
          throw new OwncloudGroupNotFoundException(groupname);
        }
        log.trace("Assign Group {} to User {}", groupname, existingUser.getUsername());
        groups.add(new OwncloudResourceData.Group(groupname));
      }
    }
    existingUser.setGroups(groups);
  }

  @Override
  public void deleteUser(String username) {
    Validate.notBlank(username);
    if (resourceService.userNotExists(username)) {
      log.error("User {} doesn't exist", username);
      throw new UsernameNotFoundException(username);
    }
    log.debug("Remove User {}", username);
    resourceService.removeUser(username);
    log.info("User {} successfully removed", username);
  }

  @Override
  public void createGroup(String groupname) {
    Validate.notBlank(groupname);
    if (resourceService.groupExists(groupname)) {
      log.error("Group {} already exists", groupname);
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    log.debug("Create Group {}", groupname);
    resourceService.addGroup(new OwncloudResourceData.Group(groupname));
    log.info("Group {} successfully created");
  }

  @Override
  public void deleteGroup(String groupname) {
    Validate.notBlank(groupname);

    log.debug("Get Information of Group {} from the Resource Service", groupname);
    Group group = resourceService.getGroup(groupname);
    OwncloudUtils.validateGroupNotNull(group, groupname);

    for (OwncloudResourceData.User user : resourceService.getUsers()) {
      if (user.getGroups() != null) {
        log.trace("Revoke Assignment of Group {} from User {}", groupname, user.getUsername());
        user.getGroups().remove(group);
      }
    }
    log.debug("Remove Group {}", groupname);
    resourceService.removeGroup(groupname);
    log.info("Group {} successfully removed", groupname);
  }

}
