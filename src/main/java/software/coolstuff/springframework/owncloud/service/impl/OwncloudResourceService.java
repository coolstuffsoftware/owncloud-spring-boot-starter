package software.coolstuff.springframework.owncloud.service.impl;

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

import lombok.Data;

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
