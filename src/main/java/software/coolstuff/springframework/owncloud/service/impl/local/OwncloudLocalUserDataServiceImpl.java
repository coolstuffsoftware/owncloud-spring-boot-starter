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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

@Slf4j
public class OwncloudLocalUserDataServiceImpl implements OwncloudLocalUserDataService, InitializingBean, DisposableBean {

  private final XmlMapper xmlMapper;

  private final ResourceLoader resourceLoader;
  private final OwncloudProperties properties;

  private Map<String, OwncloudLocalUserData.User> users = new HashMap<>();
  private Set<String> groups = new HashSet<>();

  public OwncloudLocalUserDataServiceImpl(
      Jackson2ObjectMapperBuilder builder, ResourceLoader resourceLoader, OwncloudProperties properties) {
    Validate.notNull(builder);
    xmlMapper = builder.createXmlMapper(true).build();
    xmlMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(xmlMapper.getTypeFactory()));
    this.resourceLoader = resourceLoader;
    this.properties = properties;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    Validate.notNull(resource);
    Validate.isTrue(resource.exists());
    Validate.isTrue(resource.isReadable());

    log.debug("Read the Resource {} to the Class {}", resource.getFilename(), OwncloudLocalUserData.class.getName());
    OwncloudLocalUserData resourceData = xmlMapper.readValue(resource.getInputStream(), OwncloudLocalUserData.class);
    checkGroupReferences(resourceData);

    log.trace("Clear the Users Map");
    users.clear();

    log.debug("Read the Users as a Map");
    if (CollectionUtils.isNotEmpty(resourceData.getUsers())) {
      for (OwncloudLocalUserData.User user : resourceData.getUsers()) {
        users.put(user.getUsername(), user);
      }
    }

    log.trace("Clear the Groups Map");
    groups.clear();
    log.debug("Read the Groups as a Map");
    if (CollectionUtils.isNotEmpty(resourceData.getGroups())) {
      groups.addAll(resourceData.getGroups());
    }

    log.info("User Information from Resource Location {} successfully loaded", properties.getLocation());
  }

  protected void checkGroupReferences(OwncloudLocalUserData resourceData) {
    for (OwncloudLocalUserData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      log.debug("Check, if the Groups of User {} are registered within the general Group Definitions", user.getUsername());
      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<String> unknownGroups = CollectionUtils.subtract(user.getGroups(), resourceData.getGroups());
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

    OwncloudLocalUserData resourceData = new OwncloudLocalUserData();
    log.debug("Add Users to the Synchronization Structure {}", OwncloudLocalUserData.class.getName());
    resourceData.setUsers(users.values());
    log.debug("Add Groups to the Synchronization Structure {}", OwncloudLocalUserData.class.getName());
    resourceData.setGroups(groups);

    File file = resource.getFile();
    log.info("Save changed Data to Resource {}", resource.getFilename());
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
      xmlMapper.writeValue(output, resourceData);
    }
  }

  @Override
  public OwncloudUserDetails convert(OwncloudLocalUserData.User user, boolean prefixAuthorities) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      log.trace("Put {} Owncloud-Group(s) into the Authorities-List");
      user.getGroups().stream()
          .map(this::mapToGrantedAuthority)
          .forEach(authorities::add);
    }

    log.debug("Convert User {} from {} to {}", user.getUsername(), user.getClass(), OwncloudUserDetails.class);
    return OwncloudUserDetails.builder()
                              .username(user.getUsername())
                              .enabled(user.isEnabled())
                              .displayname(user.getDisplayname())
                              .email(user.getEmail())
                              .quota(user.getQuota())
                              .authorities(authorities)
                              .build();
  }

  private GrantedAuthority mapToGrantedAuthority(String group) {
    if (StringUtils.startsWith(group, properties.getRolePrefix())) {
      return new SimpleGrantedAuthority(group);
    }
    return new SimpleGrantedAuthority(properties.getRolePrefix() + group);
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
  public Collection<OwncloudLocalUserData.User> getUsers() {
    return users.values();
  }

  @Override
  public OwncloudLocalUserData.User getUser(String username) {
    return users.get(username);
  }

  @Override
  public void addUser(OwncloudLocalUserData.User user) {
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
    return groups.contains(groupname);
  }

  @Override
  public Collection<String> getGroups() {
    return Collections.unmodifiableCollection(groups);
  }

  @Override
  public String getGroup(String groupname) {
    for (String group : groups) {
      if (group.equals(groupname)) {
        return group;
      }
    }
    return null;
  }

  @Override
  public void addGroup(String group) {
    groups.add(group);
  }

  @Override
  public void removeGroup(String groupname) {
    groups.remove(groupname);
  }
}
