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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

/**
 * If you define a URL prefixed with either <code>file:</code> or <code>classpath:</code> this Class will be available as a Service.
 *
 * By instantiating this Class as a Bean the defined Resource will be parsed by the Jackson XML Mapper and will be available in a Java-Structure of Type {@link OwncloudResourceData}.
 *
 * The defined Resource-File must have the following Structure:
 *
 * <pre>
 * &lt;owncloud&gt;
 *   &lt;users&gt;
 *     &lt;user&gt;
 *       &lt;username&gt;user1&lt;/username&gt;
 *       &lt;password&gt;password&lt;/s3cr3t&gt;
 *       &lt;enabled&gt;true&lt;/enabled&gt;
 *       &lt;displayName&gt;Mrs. User 1&lt;/displayName&gt;
 *       &lt;email&gt;user1@example.com&lt;/email&gt;
 *       &lt;groups&gt;
 *         &lt;group&gt;group1&lt;/group&gt;
 *         ...
 *       &lt;/groups&gt;
 *     &lt;/user&gt;
 *     ...
 *   &lt;/users&gt;
 *   &lt;groups&gt;
 *     &lt;group&gt;group1&lt;/group&gt;
 *     ...
 *   &lt;/groups&gt;
 * &lt;/owncloud&gt;
 * </pre>
 *
 * All Groups referenced by a User have to be defined in the <code>groups</code>-Section. There will be an {@link IllegalStateException} if there are any Groups referenced by a User which is not
 * defined in the <code>groups</code>-Section.
 *
 * Only for internal Usage.
 *
 * @see OwncloudResourceData
 * @author mufasa1976@coolstuff.software
 *
 */
@Slf4j
class OwncloudResourceService implements InitializingBean, DisposableBean {

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

  private Map<String, OwncloudResourceData.User> users = new HashMap<>();
  private Collection<OwncloudResourceData.Group> groups = new ArrayList<>();

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
    users = new HashMap<>();
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      users.put(user.getUsername(), user);
    }
    log.debug("Save the Groups as a List");
    groups = new ArrayList<>(resourceData.getGroups());
  }

  private void checkGroupReferences(OwncloudResourceData resourceData) {
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
    resourceData.setGroups(groups);

    File file = resource.getFile();
    log.info("Save changed Data to Resource {}", resource.getFilename());
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
      messageConverter.getObjectMapper().writeValue(output, resourceData);
    }
  }

  /**
   * Checks, if the defined Location is a Resource.
   * <p/>
   * This will be done by checking, if the Location starts either with <code>file:</code> or <code>classpath</code>
   *
   * @param location
   *          Location to be checked
   * @return
   *         <ul>
   *         <li>true ... Location is a Resource</li>
   *         <li>false ... Location is possible a URL</li>
   *         </ul>
   */
  public static boolean isResourceInsteadOfUrl(String location) {
    return StringUtils.startsWith(location, "file:") || StringUtils.startsWith(location, "classpath:");
  }

  /**
   * Checks, if the defined Location is not a Resource.
   * <p/>
   * This will be done by checking, if the Location starts either with <code>file:</code> or <code>classpath</code>
   *
   * @param location
   *          Location to be checked
   * @return
   *         <ul>
   *         <li>true ... Location is possible a URL</li>
   *         <li>false ... Location is a Resource</li>
   *         </ul>
   */
  public static boolean isNoResource(String location) {
    return !isResourceInsteadOfUrl(location);
  }

  /**
   * Checks, if the given Credentials are available in the Resource.
   * <p/>
   * If either the User hasn't been found or an invalid Password has been given then the authentication is negative (returns <code>false</code>)
   *
   * @param username
   *          Username
   * @param password
   *          Password
   * @return
   *         <ul>
   *         <li>true ... User found and the Password is correct</li>
   *         <li>false ... either the User doesn&apos;t exist or the Password is wrong</li>
   *         </ul>
   */
  public boolean authenticate(String username, String password) {
    OwncloudResourceData.User user = users.get(username);
    return StringUtils.equals(password, user.getPassword());
  }

  /**
   * Get the Details of the User.
   * <p/>
   * The Details will <strong>always</strong> be returned without a Password.
   *
   * @param username
   *          Username
   * @return Details of the User
   */
  public OwncloudUserDetails getUser(String username) {
    OwncloudResourceData.User user = users.get(username);
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }
    return convert(user);
  }

  private OwncloudUserDetails convert(OwncloudResourceData.User user) {
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
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
    if (owncloudGrantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(authorities));
    }
    return userDetails;
  }

  /**
   * Get all Users.
   * <p/>
   * The Search can be filtered
   *
   * @param filter
   *          Filter Criteria
   * @return List of Users
   */
  public List<String> getAllUsers(String filter) {
    List<String> filteredUsers = new ArrayList<>();
    for (OwncloudResourceData.User user : users.values()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayName(), filter)) {
        filteredUsers.add(user.getUsername());
      }
    }
    return filteredUsers;
  }

  /**
   * Get all Groups.
   * <p/>
   * The Search can be filtered.
   *
   * @param filter
   *          Filter Criteria
   * @return List of Groups
   */
  public List<String> getAllGroups(String filter) {
    List<String> filteredGroups = new ArrayList<>();
    for (OwncloudResourceData.Group group : groups) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(group.getGroup(), filter)) {
        filteredGroups.add(group.getGroup());
      }
    }
    return filteredGroups;
  }

  /**
   * Get all Users which are member of a Group
   *
   * @param groupname
   *          Name of the Group
   * @return List of Users
   */
  public List<String> getAllMembersOfGroup(String groupname) {
    checkGroupExistence(groupname);
    List<String> members = new ArrayList<>();
    for (OwncloudResourceData.User user : users.values()) {
      addWhenMemberOfGroup(groupname, members, user);
    }
    return members;
  }

  private void checkGroupExistence(String groupname) {
    if (notExistsGroup(groupname)) {
      throw new OwncloudGroupNotFoundException(groupname);
    }
  }

  private boolean notExistsGroup(String groupname) {
    return !existsGroup(groupname);
  }

  private boolean existsGroup(String groupname) {
    for (OwncloudResourceData.Group group : groups) {
      if (StringUtils.equals(groupname, group.getGroup())) {
        return true;
      }
    }
    return false;
  }

  private void addWhenMemberOfGroup(String groupname, List<String> members, OwncloudResourceData.User user) {
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        if (StringUtils.equals(groupname, group.getGroup())) {
          members.add(user.getUsername());
          break;
        }
      }
    }
  }

  /**
   * Get all Groups of a User
   *
   * @param username
   *          Name of the User
   * @return List of Groups
   */
  public List<String> getGroupsOfUser(String username) {
    OwncloudResourceData.User user = users.get(username);
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  public OwncloudUserDetails saveUser(OwncloudModificationUser user) {
    OwncloudResourceData.User existingUser = users.get(user.getUsername());

    if (existingUser == null) {
      Validate.notBlank(user.getPassword());

      existingUser = new OwncloudResourceData.User();
      existingUser.setUsername(user.getUsername());
      existingUser.setPassword(user.getPassword());
      users.put(existingUser.getUsername(), existingUser);
    }

    existingUser.setDisplayName(user.getDisplayName());
    existingUser.setEmail(user.getEmail());
    existingUser.setEnabled(user.isEnabled());

    manageGroups(existingUser, user);

    OwncloudUserDetails userDetails = convert(users.get(user.getUsername()));
    userDetails.setPassword(user.getPassword());
    return userDetails;
  }

  private void manageGroups(OwncloudResourceData.User existingUser, OwncloudModificationUser newUser) {
    List<OwncloudResourceData.Group> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(newUser.getGroups())) {
      for (String group : newUser.getGroups()) {
        checkGroupExistence(group);
        groups.add(new OwncloudResourceData.Group(group));
      }
    }
    existingUser.setGroups(groups);
  }

  public void deleteUser(String username) {
    if (!users.containsKey(username)) {
      throw new UsernameNotFoundException(username);
    }
    users.remove(username);
  }

  public void createGroup(String groupname) {
    if (groups.contains(groupname)) {
      throw new OwncloudGroupAlreadyExistsException(groupname);
    }
    groups.add(new OwncloudResourceData.Group(groupname));
  }

  public void deleteGroup(String groupname) {
    if (!groups.contains(groupname)) {
      throw new OwncloudGroupNotFoundException(groupname);
    }

    for (OwncloudResourceData.User user : users.values()) {
      user.getGroups().remove(groupname);
    }
    groups.remove(groupname);
  }

  @lombok.Data
  @JacksonXmlRootElement(localName = "owncloud")
  private static class OwncloudResourceData {

    @lombok.Data
    @AllArgsConstructor
    @JacksonXmlRootElement(localName = "group")
    public static class Group {

      @JacksonXmlText
      private String group;

    }

    @lombok.Data
    @JacksonXmlRootElement(localName = "user")
    public static class User {

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

      @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
      @JacksonXmlProperty(localName = "group")
      private List<Group> groups;
    }

    @JacksonXmlElementWrapper(localName = "users", useWrapping = true)
    @JacksonXmlProperty(localName = "user")
    private Collection<User> users;

    @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
    @JacksonXmlProperty(localName = "group")
    private Collection<Group> groups;
  }

}
