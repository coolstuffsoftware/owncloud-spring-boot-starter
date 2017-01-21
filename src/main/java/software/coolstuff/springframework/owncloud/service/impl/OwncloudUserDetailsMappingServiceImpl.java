package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;

@Slf4j
class OwncloudUserDetailsMappingServiceImpl implements OwncloudUserDetailsMappingService {

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Override
  public void mapGrantedAuthorities(OwncloudUserDetails userDetails) {
    if (owncloudGrantedAuthoritiesMapper != null) {
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), OwncloudGrantedAuthoritiesMapper.class, owncloudGrantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(owncloudGrantedAuthoritiesMapper.mapAuthorities(userDetails.getUsername(), userDetails.getAuthorities()));
    } else if (grantedAuthoritiesMapper != null) {
      log.debug("Map the Authorities of User {} by {} ({})", userDetails.getUsername(), GrantedAuthoritiesMapper.class, grantedAuthoritiesMapper.getClass());
      userDetails.setAuthorities(grantedAuthoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
    }
  }
}
