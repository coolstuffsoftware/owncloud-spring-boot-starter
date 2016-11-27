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
    Resource resource = resourceLoader.getResource(properties.getUrl());
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

  public static boolean isResourceInsteadOfUrl(String possibleUrl) {
    return StringUtils.startsWith(possibleUrl, "file:") || StringUtils.startsWith(possibleUrl, "classpath:");
  }

  public static boolean isNoResource(String possibleResource) {
    return !isResourceInsteadOfUrl(possibleResource);
  }

  public boolean authenticate(String username, String password) {
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.equals(username, user.getUsername()) && StringUtils.equals(password, user.getPassword())) {
        return true;
      }
    }
    return false;
  }

  public OwncloudUserDetails getUser(String username) {
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
        .password(user.getPassword())
        .enabled(user.isEnabled())
        .displayName(user.getDisplayName())
        .email(user.getEmail())
        .authorities(authorities)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
  }

  public List<OwncloudUserDetails> getAllUsers(String filter) {
    List<OwncloudUserDetails> users = new ArrayList<>();
    for (OwncloudResourceData.User user : resourceData.getUsers()) {
      if (StringUtils.isBlank(filter) || StringUtils.contains(user.getDisplayName(), filter)) {
        users.add(convertToOwncloudUserDetailsFrom(user));
      }
    }
    return users;
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
