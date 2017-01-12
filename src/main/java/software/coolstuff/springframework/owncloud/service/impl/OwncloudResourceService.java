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
package software.coolstuff.springframework.owncloud.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OwncloudResourceService {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  private Map<String, OwncloudResourceData.User> users = new HashMap<>();
  private Map<String, OwncloudResourceData.Group> groups = new HashMap<>();

  @PostConstruct
  public void afterPropertiesSet() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    Validate.notNull(resource);
    Validate.isTrue(resource.exists());
    Validate.isTrue(resource.isReadable());

    log.debug("Read the Resource {} to the Class {}", resource.getFilename(), OwncloudResourceData.class.getName());
    OwncloudResourceData resourceData = messageConverter.getObjectMapper().readValue(resource.getInputStream(),
        OwncloudResourceData.class);
    checkGroupReferences(resourceData);

    log.trace("Clear the Users Map");
    users.clear();

    log.debug("Read the Users as a Map");
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      users.put(user.getUsername(), user);
    }

    log.trace("Clear the Groups Map");
    groups.clear();

    log.debug("Read the Groups as a Map");
    for (OwncloudResourceData.Group group : resourceData.getGroups()) {
      groups.put(group.getGroup(), group);
    }

    log.info("User Information from Resource Location {} successfully loaded", properties.getLocation());
  }

  protected void checkGroupReferences(OwncloudResourceData resourceData) {
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      log.debug("Check, if the Groups of User {} are registered within the general Group Definitions", user.getUsername());
      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<OwncloudResourceData.Group> unknownGroups = CollectionUtils.subtract(user.getGroups(), resourceData.getGroups());
        final String exceptionMessage = String.format(
            "User %s has unknown Groups defined: %s. Please define these Groups within <groups> or remove it from the User",
            user.getUsername(),
            unknownGroups);
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      }
    }
  }

  @PreDestroy
  public void destroy() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    if (!(resource instanceof UrlResource)) {
      log.debug("Resource {} is not of Type {}. Can't synchronize changed Data", resource.getFilename(), UrlResource.class.getName());
      return;
    }

    OwncloudResourceData resourceData = new OwncloudResourceData();
    log.debug("Add Users to the Synchronization Structure {}", OwncloudResourceData.class.getName());
    resourceData.setUsers(users.values());
    log.debug("Add Gropus to the Synchronization Structure {}", OwncloudResourceData.class.getName());
    resourceData.setGroups(groups.values());

    File file = resource.getFile();
    log.info("Save changed Data to Resource {}", resource.getFilename());
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
      messageConverter.getObjectMapper().writeValue(output, resourceData);
    }
  }

  public boolean userNotExists(String username) {
    return !userExists(username);
  }

  public boolean userExists(String username) {
    return users.containsKey(username);
  }

  public Collection<OwncloudResourceData.User> getUsers() {
    return users.values();
  }

  public OwncloudResourceData.User getUser(String username) {
    return users.get(username);
  }

  public void addUser(OwncloudResourceData.User user) {
    users.put(user.getUsername(), user);
  }

  public void removeUser(String username) {
    users.remove(username);
  }

  public boolean groupNotExists(String groupname) {
    return !groupExists(groupname);
  }

  public boolean groupExists(String groupname) {
    return groups.containsKey(groupname);
  }

  public Collection<OwncloudResourceData.Group> getGroups() {
    return groups.values();
  }

  public OwncloudResourceData.Group getGroup(String groupname) {
    return groups.get(groupname);
  }

  public void addGroup(OwncloudResourceData.Group group) {
    groups.put(group.getGroup(), group);
  }

  public void removeGroup(String groupname) {
    groups.remove(groupname);
  }

  @lombok.Data
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @lombok.Builder
  @JacksonXmlRootElement(localName = "owncloud")
  static class OwncloudResourceData {

    @lombok.Data
    @AllArgsConstructor
    @JacksonXmlRootElement(localName = "group")
    static class Group {

      @JacksonXmlText
      private String group;

    }

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @lombok.Builder
    @JacksonXmlRootElement(localName = "user")
    static class User {

      @NotNull
      @JacksonXmlProperty(localName = "username")
      private String username;

      @JacksonXmlProperty(localName = "password")
      private String password;

      @JacksonXmlProperty(localName = "enabled")
      private boolean enabled = true;

      @JacksonXmlProperty(localName = "displayname")
      private String displayname;

      @JacksonXmlProperty(localName = "email")
      private String email;

      @Singular
      @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
      @JacksonXmlProperty(localName = "group")
      private List<Group> groups;
    }

    @Singular
    @JacksonXmlElementWrapper(localName = "users", useWrapping = true)
    @JacksonXmlProperty(localName = "user")
    private Collection<User> users;

    @Singular
    @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
    @JacksonXmlProperty(localName = "group")
    private Collection<Group> groups;
  }

}
