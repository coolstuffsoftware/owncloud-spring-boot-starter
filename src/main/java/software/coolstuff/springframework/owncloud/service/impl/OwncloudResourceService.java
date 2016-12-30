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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudAuthentication;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@Slf4j
class OwncloudResourceService implements InitializingBean, DisposableBean, AuthenticationProvider, UserDetailsService {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

  private Map<String, OwncloudResourceData.User> users = new HashMap<>();
  private Map<String, OwncloudResourceData.Group> groups = new HashMap<>();

  @Override
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

    log.debug("Save the Users as a Map");
    users.clear();
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      users.put(user.getUsername(), user);
    }

    log.debug("Save the Groups as a List");
    groups.clear();
    for (OwncloudResourceData.Group group : resourceData.getGroups()) {
      groups.put(group.getGroup(), group);
    }
  }

  protected void checkGroupReferences(OwncloudResourceData resourceData) {
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      log.debug("Check, if the Groups of User {} are registered within the general Group Definitions",
          user.getUsername());
      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<OwncloudResourceData.Group> unknownGroups = CollectionUtils.subtract(user.getGroups(),
            resourceData.getGroups());
        throw new IllegalStateException("User " + user.getUsername() + " has unknown Groups defined: " + unknownGroups
            + " Please define these Groups within <groups> or remove these Groups from the User");
      }
    }
  }

  @Override
  public void destroy() throws Exception {
    log.debug("Load Resource from Location {}", properties.getLocation());
    Resource resource = resourceLoader.getResource(properties.getLocation());
    if (!(resource instanceof UrlResource)) {
      log.debug("Resource {} is not of Type {}. Can't synchronize changed Data", resource.getFilename(),
          UrlResource.class.getName());
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

  @Override
  public boolean supports(Class<?> authentication) {
    return OwncloudUtils.isAuthenticationClassSupported(authentication);
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (StringUtils.isBlank(authentication.getName())) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    if (authentication.getCredentials() == null) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    OwncloudResourceData.User user = users.get(username);
    if (user == null) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    if (!StringUtils.equals(password, user.getPassword())) {
      throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad Credentials"));
    }

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, password));

    OwncloudUserDetails owncloudUserDetails = (OwncloudUserDetails) loadUserByUsername(username);
    owncloudUserDetails.setPassword(password);

    return new OwncloudAuthentication(owncloudUserDetails);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    OwncloudResourceData.User user = users.get(username);
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
    return convert(user);
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

  public OwncloudUserDetails convert(OwncloudResourceData.User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group.getGroup()));
        groups.add(group.getGroup());
      }
    }

    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(user.getUsername())
        .enabled(user.isEnabled())
        .displayName(user.getDisplayName())
        .email(user.getEmail())
        .groups(groups)
        .authorities(authorities)
        .build();
    if (owncloudGrantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(authorities));
    }
    return userDetails;
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

      @JacksonXmlProperty(localName = "displayName")
      private String displayName;

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
