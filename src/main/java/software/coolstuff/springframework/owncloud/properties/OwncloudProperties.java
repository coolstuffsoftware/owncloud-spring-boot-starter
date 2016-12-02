package software.coolstuff.springframework.owncloud.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "owncloud")
public class OwncloudProperties {
  private String location;
  private String username;
  private String password;
  private boolean enableModifications = false;
}
