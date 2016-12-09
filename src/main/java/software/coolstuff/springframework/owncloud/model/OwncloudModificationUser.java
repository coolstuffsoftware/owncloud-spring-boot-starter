package software.coolstuff.springframework.owncloud.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.GrantedAuthority;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OwncloudModificationUser {
  private String username;
  private String password;

  private boolean enabled = true;

  private String displayName;
  private String email;

  @Singular("group")
  private List<String> groups = new ArrayList<>();

  public static class OwncloudModificationUserBuilder {
    private boolean enabled = true;
  }

  public OwncloudModificationUser(OwncloudUserDetails userDetails) {
    Validate.notNull(userDetails);

    setUsername(userDetails.getUsername());
    setPassword(userDetails.getPassword());

    setEnabled(userDetails.isEnabled());

    setDisplayName(userDetails.getDisplayName());
    setEmail(userDetails.getEmail());

    if (CollectionUtils.isNotEmpty(userDetails.getAuthorities())) {
      for (GrantedAuthority authority : userDetails.getAuthorities()) {
        groups.add(authority.getAuthority());
      }
    }
  }
}
