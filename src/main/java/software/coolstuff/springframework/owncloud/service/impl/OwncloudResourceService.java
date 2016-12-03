package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.AllArgsConstructor;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.properties.OwncloudProperties;

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
class OwncloudResourceService implements InitializingBean, DisposableBean {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  private Map<String, OwncloudResourceData.User> users = new HashMap<>();
  private Collection<OwncloudResourceData.Group> groups = new ArrayList<>();

  @Override
  public void afterPropertiesSet() throws Exception {
    Resource resource = resourceLoader.getResource(properties.getLocation());
    Validate.notNull(resource);
    Validate.isTrue(resource.exists());
    Validate.isTrue(resource.isReadable());

    OwncloudResourceData resourceData = messageConverter.getObjectMapper().readValue(resource.getInputStream(), OwncloudResourceData.class);
    checkGroupReferences(resourceData);

    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      users.put(user.getUsername(), user);
    }
    groups.addAll(resourceData.getGroups());
  }

  private void checkGroupReferences(OwncloudResourceData resourceData) {
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<OwncloudResourceData.Group> unknownGroups = CollectionUtils.subtract(user.getGroups(), resourceData.getGroups());
        throw new IllegalStateException(
            "User " + user.getUsername() + " has unknown Groups defined: " + unknownGroups + " Please define these Groups within <groups> or remove these Groups from the User");
      }
    }
  }

  @Override
  public void destroy() throws Exception {
    Resource resource = resourceLoader.getResource(properties.getLocation());
    if (!(resource instanceof WritableResource)) {
      return;
    }

    OwncloudResourceData resourceData = new OwncloudResourceData();
    resourceData.setUsers(users.values());
    resourceData.setGroups(groups);

    WritableResource writableResource = (WritableResource) resource;
    messageConverter.getObjectMapper().writeValue(writableResource.getOutputStream(), resourceData);
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
    if (user == null) {
      return false;
    }
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
      return null;
    }
    return convertToOwncloudUserDetailsFrom(user);
  }

  private OwncloudUserDetails convertToOwncloudUserDetailsFrom(OwncloudResourceData.User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group.getGroup()));
      }
    }
    return OwncloudUserDetails.builder()
        .username(user.getUsername())
        .enabled(user.isEnabled())
        .displayName(user.getDisplayName())
        .email(user.getEmail())
        .authorities(authorities)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
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
    List<String> members = new ArrayList<>();
    for (OwncloudResourceData.User user : users.values()) {
      if (CollectionUtils.isNotEmpty(user.getGroups()) && user.getGroups().contains(groupname)) {
        members.add(user.getUsername());
      }
    }
    return members;
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
    if (user == null) {
      return groups;
    }
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      for (OwncloudResourceData.Group group : user.getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  public OwncloudUserDetails saveUser(OwncloudUserDetails userDetails) {
    OwncloudResourceData.User user = users.get(userDetails.getUsername());

    if (user == null) {
      Validate.notBlank(userDetails.getPassword());

      user = new OwncloudResourceData.User();
      user.setUsername(userDetails.getUsername());
      users.put(user.getUsername(), user);
    }

    user.setDisplayName(userDetails.getDisplayName());
    user.setEmail(userDetails.getEmail());
    user.setEnabled(userDetails.isEnabled());
    if (StringUtils.isNotBlank(userDetails.getPassword())) {
      user.setPassword(userDetails.getPassword());
    }

    manageGroups(user, userDetails);

    return convertToOwncloudUserDetailsFrom(users.get(userDetails.getUsername()));
  }

  private void manageGroups(OwncloudResourceService.OwncloudResourceData.User user, OwncloudUserDetails userDetails) {
    List<OwncloudResourceData.Group> existingGroups = new ArrayList<>(user.getGroups());
    for (GrantedAuthority authority : userDetails.getAuthorities()) {
      if (existingGroups.contains(authority.getAuthority())) {
        existingGroups.remove(authority.getAuthority());
        continue;
      }

      if (!groups.contains(authority.getAuthority())) {
        throw new OwncloudGroupNotFoundException(authority.getAuthority());
      }
      user.getGroups().add(new OwncloudResourceData.Group(authority.getAuthority()));
    }

    for (OwncloudResourceData.Group removableGroup : existingGroups) {
      user.getGroups().remove(removableGroup);
    }
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
  @XmlRootElement(name = "owncloud")
  private static class OwncloudResourceData {

    @lombok.Data
    @AllArgsConstructor
    public static class Group {
      private String group;
    }

    @lombok.Data
    public static class User {

      @NotNull
      private String username;
      private String password;
      private boolean enabled = true;
      private String displayName;
      private String email;
      private List<Group> groups;
    }

    private Collection<User> users;
    private Collection<Group> groups;
  }

}
