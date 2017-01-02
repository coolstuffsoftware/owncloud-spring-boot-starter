package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

class OwncloudUserDetailsConversionService {

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  protected OwncloudUserDetails convert(String username, Ocs.User user, Ocs.Groups groupsFromBackend) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (isGroupAvailable(groupsFromBackend)) {
      for (Ocs.Groups.Data.Group group : groupsFromBackend.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(group.getGroup()));
        groups.add(group.getGroup());
      }
    }

    OwncloudUserDetails userDetails = OwncloudUserDetails.builder()
        .username(username)
        .enabled(user.getData().isEnabled())
        .displayName(user.getData().getDisplayname())
        .email(user.getData().getEmail())
        .groups(groups)
        .authorities(authorities)
        .build();
    mapGrantedAuthorities(userDetails);
    return userDetails;
  }

  private void mapGrantedAuthorities(OwncloudUserDetails userDetails) {
    if (owncloudGrantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
    }
  }

  private boolean isGroupAvailable(Ocs.Groups groups) {
    return groups != null && groups.getData() != null && groups.getData().getGroups() != null;
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
    mapGrantedAuthorities(userDetails);
    return userDetails;
  }

}
