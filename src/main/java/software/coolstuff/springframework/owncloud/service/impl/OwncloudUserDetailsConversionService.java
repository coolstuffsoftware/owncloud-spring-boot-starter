package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudResourceService.OwncloudResourceData;

@Slf4j
class OwncloudUserDetailsConversionService {

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  OwncloudUserDetails convert(String username, Ocs.User user, Ocs.Groups groupsFromBackend) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (isAnyOwncloudGroupAvailable(groupsFromBackend)) {
      log.trace("Put {} Owncloud-Group(s) into the Authorities- and Group-List");
      for (Ocs.Groups.Data.Group owncloudGroup : groupsFromBackend.getData().getGroups()) {
        authorities.add(new SimpleGrantedAuthority(owncloudGroup.getGroup()));
        groups.add(owncloudGroup.getGroup());
      }
    }

    log.debug("Convert User {} from {} to {}", username, user.getClass(), OwncloudUserDetails.class);
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
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), OwncloudGrantedAuthoritiesMapper.class, owncloudGrantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), GrantedAuthoritiesMapper.class, grantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
    }
  }

  private boolean isAnyOwncloudGroupAvailable(Ocs.Groups groups) {
    return groups != null && groups.getData() != null && groups.getData().getGroups() != null;
  }

  OwncloudUserDetails convert(OwncloudResourceData.User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(user.getGroups())) {
      log.trace("Put {} Owncloud-Group(s) into the Authorities- and Group-List");
      for (OwncloudResourceData.Group ownclougGroup : user.getGroups()) {
        authorities.add(new SimpleGrantedAuthority(ownclougGroup.getGroup()));
        groups.add(ownclougGroup.getGroup());
      }
    }

    log.debug("Convert User {} from {} to {}", user.getUsername(), user.getClass(), OwncloudUserDetails.class);
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
