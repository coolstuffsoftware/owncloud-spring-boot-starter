package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import lombok.Data;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

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
class OwncloudResourceService implements InitializingBean {

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private MappingJackson2XmlHttpMessageConverter messageConverter;

  private OwncloudResourceData resourceData;

  @Override
  public void afterPropertiesSet() throws Exception {
    Resource resource = resourceLoader.getResource(properties.getLocation());
    Validate.notNull(resource);
    Validate.isTrue(resource.exists());
    Validate.isTrue(resource.isReadable());

    resourceData = messageConverter.getObjectMapper().readValue(resource.getInputStream(), OwncloudResourceData.class);
    checkGroupReferences();
  }

  private void checkGroupReferences() {
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isEmpty(user.getGroups())) {
        continue;
      }

      if (!CollectionUtils.isSubCollection(user.getGroups(), resourceData.getGroups())) {
        Collection<String> unknownGroups = CollectionUtils.subtract(user.getGroups(), resourceData.getGroups());
        throw new IllegalStateException(
            "User " + user.getUsername() + " has unknown Groups defined: " + unknownGroups + " Please define these Groups within <groups> or remove these Groups from the User");
      }
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
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.equals(username, user.getUsername()) && StringUtils.equals(password, user.getPassword())) {
        return true;
      }
    }
    return false;
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
    if (CollectionUtils.isEmpty(resourceData.getUsers())) {
      return null;
    }

    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.equals(username, user.getUsername())) {
        return convertToOwncloudUserDetailsFrom(user);
      }
    }

    return null;
  }

  private OwncloudUserDetails convertToOwncloudUserDetailsFrom(OwncloudResourceData.User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      authorities.addAll(AuthorityUtils.createAuthorityList(user.getGroups().toArray(new String[] {})));
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
    List<String> users = new ArrayList<>();
    if (CollectionUtils.isEmpty(resourceData.getUsers())) {
      return users;
    }

    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayName(), filter)) {
        users.add(user.getUsername());
      }
    }

    return users;
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
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isEmpty(resourceData.getGroups())) {
      return groups;
    }

    for (String group : resourceData.getGroups()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(group, filter)) {
        groups.add(group);
      }
    }

    return groups;
  }

  /**
   * Get all Users which are member of a Group
   *
   * @param groupname
   *          Name of the Group
   * @return List of Users
   */
  public List<String> getAllMembersOfGroup(String groupname) {
    List<String> users = new ArrayList<>();
    if (CollectionUtils.isEmpty(resourceData.getGroups())) {
      return users;
    }

    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (CollectionUtils.isNotEmpty(user.getGroups()) && user.getGroups().contains(groupname)) {
        users.add(user.getUsername());
      }
    }

    return users;
  }

  /**
   * Get all Groups of a User
   *
   * @param username
   *          Name of the User
   * @return List of Groups
   */
  public List<String> getGroupsOfUser(String username) {
    if (CollectionUtils.isEmpty(resourceData.getUsers())) {
      return new ArrayList<>();
    }

    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.equals(username, username)) {
        return new ArrayList<>(user.getGroups());
      }
    }

    return new ArrayList<>();
  }

  @Data
  @XmlRootElement(name = "owncloud")
  private static class OwncloudResourceData {

    @Data
    public static class User {
      @NotNull
      private String username;
      private String password;
      private boolean enabled = true;
      private String displayName;
      private String email;
      @XmlElementWrapper
      private List<String> groups;
    }

    @XmlElementWrapper
    private List<User> users;

    @XmlElementWrapper
    private List<String> groups;
  }

}
