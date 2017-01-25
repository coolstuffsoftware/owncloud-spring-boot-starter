/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl.local;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsMappingService;

@Slf4j
class OwncloudLocalDataServiceImpl implements OwncloudLocalDataService, InitializingBean, DisposableBean {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  @Autowired
  private OwncloudUserDetailsMappingService owncloudUserDetailsMappingService;

  private Map<String, OwncloudLocalData.User> users = new HashMap<>();
  private Map<String, OwncloudLocalData.Group> groups = new HashMap<>();

  @Override
  public void afterPropertiesSet() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    Validate.notNull(resource);
    Validate.isTrue(resource.exists());
    Validate.isTrue(resource.isReadable());

    log.debug("Read the Resource {} to the Class {}", resource.getFilename(), OwncloudLocalData.class.getName());
    OwncloudLocalData resourceData = messageConverter.getObjectMapper().readValue(resource.getInputStream(),
        OwncloudLocalData.class);
    checkGroupReferences(resourceData);

    log.trace("Clear the Users Map");
    users.clear();

    log.debug("Read the Users as a Map");
    for (OwncloudLocalData.User user : resourceData.getUsers()) {
      users.put(user.getUsername(), user);
    }

    log.trace("Clear the Groups Map");
    groups.clear();

    log.debug("Read the Groups as a Map");
    for (OwncloudLocalData.Group group : resourceData.getGroups()) {
      groups.put(group.getGroup(), group);
    }

    log.info("User Information from Resource Location {} successfully loaded", properties.getLocation());
  }

  protected void checkGroupReferences(OwncloudLocalData resourceData) {
    for (OwncloudLocalData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      log.debug("Check, if the Groups of User {} are registered within the general Group Definitions", user.getUsername());
      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<OwncloudLocalData.Group> unknownGroups = CollectionUtils.subtract(user.getGroups(), resourceData.getGroups());
        final String exceptionMessage = String.format(
            "User %s has unknown Groups defined: %s. Please define these Groups within <groups> or remove it from the User",
            user.getUsername(),
            unknownGroups);
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      }
    }
  }

  @Override
  public void destroy() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    if (!(resource instanceof UrlResource)) {
      log.debug("Resource {} is not of Type {}. Can't synchronize changed Data", resource.getFilename(), UrlResource.class.getName());
      return;
    }

    OwncloudLocalData resourceData = new OwncloudLocalData();
    log.debug("Add Users to the Synchronization Structure {}", OwncloudLocalData.class.getName());
    resourceData.setUsers(users.values());
    log.debug("Add Gropus to the Synchronization Structure {}", OwncloudLocalData.class.getName());
    resourceData.setGroups(groups.values());

    File file = resource.getFile();
    log.info("Save changed Data to Resource {}", resource.getFilename());
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
      messageConverter.getObjectMapper().writeValue(output, resourceData);
    }
  }

  @Override
  public OwncloudUserDetails convert(OwncloudLocalData.User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      log.trace("Put {} Owncloud-Group(s) into the Authorities- and Group-List");
      for (OwncloudLocalData.Group ownclougGroup : user.getGroups()) {
        authorities.add(new SimpleGrantedAuthority(ownclougGroup.getGroup()));
        groups.add(ownclougGroup.getGroup());
      }
    }

    log.debug("Convert User {} from {} to {}", user.getUsername(), user.getClass(), OwncloudUserDetails.class);
    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(user.getUsername())
        .enabled(user.isEnabled())
        .displayname(user.getDisplayname())
        .email(user.getEmail())
        .groups(groups)
        .authorities(authorities)
        .build();
    owncloudUserDetailsMappingService.mapGrantedAuthorities(userDetails);
    return userDetails;
  }

  @Override
  public boolean userNotExists(String username) {
    return !userExists(username);
  }

  @Override
  public boolean userExists(String username) {
    return users.containsKey(username);
  }

  @Override
  public Collection<OwncloudLocalData.User> getUsers() {
    return users.values();
  }

  @Override
  public OwncloudLocalData.User getUser(String username) {
    return users.get(username);
  }

  @Override
  public void addUser(OwncloudLocalData.User user) {
    users.put(user.getUsername(), user);
  }

  @Override
  public void removeUser(String username) {
    users.remove(username);
  }

  @Override
  public boolean groupNotExists(String groupname) {
    return !groupExists(groupname);
  }

  @Override
  public boolean groupExists(String groupname) {
    return groups.containsKey(groupname);
  }

  @Override
  public Collection<OwncloudLocalData.Group> getGroups() {
    return groups.values();
  }

  @Override
  public OwncloudLocalData.Group getGroup(String groupname) {
    return groups.get(groupname);
  }

  @Override
  public void addGroup(OwncloudLocalData.Group group) {
    groups.put(group.getGroup(), group);
  }

  @Override
  public void removeGroup(String groupname) {
    groups.remove(groupname);
  }
}
