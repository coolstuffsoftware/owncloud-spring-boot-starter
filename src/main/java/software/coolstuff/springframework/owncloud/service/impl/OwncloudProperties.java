package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "owncloud")
public class OwncloudProperties {
  private String location;
  private boolean authenticateWithAdministrator = true;
  private String username;
  private String password;
  private boolean enableModification = false;
}
