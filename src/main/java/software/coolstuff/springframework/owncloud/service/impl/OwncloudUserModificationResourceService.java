package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.RequiredArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData.Group;

@RequiredArgsConstructor
class OwncloudUserModificationResourceService implements OwncloudUserModificationService {

  private final OwncloudResourceService resourceService;

  @Autowired
  private OwncloudUserDetailsConversionService conversionService;

  @Override
  public OwncloudUserDetails saveUser(OwncloudModificationUser modificationUser) {
    Validate.notNull(modificationUser);
    Validate.notBlank(modificationUser.getUsername());

    OwncloudResourceData.User existingUser = resourceService.getUser(modificationUser.getUsername());

    if (existingUser == null) {
      Validate.notBlank(modificationUser.getPassword());

      existingUser = new OwncloudResourceData.User();
      existingUser.setUsername(modificationUser.getUsername());
      existingUser.setPassword(modificationUser.getPassword());
      resourceService.addUser(existingUser);
    }

    existingUser.setDisplayName(modificationUser.getDisplayName());
    existingUser.setEmail(modificationUser.getEmail());
    existingUser.setEnabled(modificationUser.isEnabled());

    manageGroups(existingUser, modificationUser);

    OwncloudUserDetails changedUserDetails = conversionService.convert(existingUser);
    changedUserDetails.setPassword(modificationUser.getPassword());
    return changedUserDetails;
  }

  private void manageGroups(OwncloudResourceData.User existingUser, OwncloudModificationUser newUser) {
    List<OwncloudResourceData.Group> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      for (String groupname : newUser.getGroups()) {
        if (resourceService.groupNotExists(groupname)) {
          throw new OwncloudGroupNotFoundException(groupname);
        }
        groups.add(new OwncloudResourceData.Group(groupname));
      }
    }
    existingUser.setGroups(groups);
  }

  @Override
  public void deleteUser(String username) {
    Validate.notBlank(username);
    if (resourceService.userNotExists(username)) {
      throw new UsernameNotFoundException(username);
    }
    resourceService.removeUser(username);
  }

  @Override
  public void createGroup(String groupname) {
    Validate.notBlank(groupname);
    if (resourceService.groupExists(groupname)) {
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    resourceService.addGroup(new OwncloudResourceData.Group(groupname));
  }

  @Override
  public void deleteGroup(String groupname) {
    Validate.notBlank(groupname);

    Group group = resourceService.getGroup(groupname);
    OwncloudUtils.validateGroupNotNull(group, groupname);

    for (OwncloudResourceData.User user : resourceService.getUsers()) {
      if (user.getGroups() != null) {
        user.getGroups().remove(group);
      }
    }
    resourceService.removeGroup(groupname);
  }

}
