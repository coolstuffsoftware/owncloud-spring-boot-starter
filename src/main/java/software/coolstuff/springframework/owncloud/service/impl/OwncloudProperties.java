package software.coolstuff.springframework.owncloud.service.impl;

import java.net.URL;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "owncloud")
public class OwncloudProperties {
  private URL url;
  private boolean enableAuthentication = false;
  private String username;
  private String password;
  private String rolePrefix = "ROLE_";
}
