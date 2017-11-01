/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.model;

import lombok.*;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static lombok.AccessLevel.NONE;

/**
 * This Class will be used for any User Modifications by
 * <code>OwncloudUserModificationService.saveUser(OwncloudModificationUser)</code>
 *
 * @author mufasa1976
 * @since 1.0.0
 */
@Data
@RequiredArgsConstructor
public class OwncloudModificationUser {

  /**
   * Username of the User to be saved
   *
   * @return Username of the User to be saved
   */
  private final String username;

  /**
   * Password of the new User.
   * <p/>
   * This Paramter will only be used, when the User will be created
   *
   * @param password
   *            Password of the new User
   * @return Password of the new User
   */
  private String password;

  /**
   * State of the User to be modified.
   *
   * @param enabled
   *            set the User to be enabled (<code>true</code>) or disabled
   *            (<code>false</code>)
   * @return is the User enabled (<code>true</code>) or disabled
   *         (<code>false</code>)
   */
  private boolean enabled = true;

  /**
   * Display Name of the User to be modified.
   *
   * @param displayname
   *            Display Name of the User to be modified
   * @return modified Display Name
   */
  private String displayname;

  /**
   * Email of the User to be modified.
   *
   * @param email
   *            Email of the User to be modified
   * @return modified Email
   */
  private String email;

  /**
   * Quota of the User to be modified (in Bytes).
   *
   * @since 1.2.0
   * @param quota
   *            Quota of the User to be modified (in Bytes)
   * @return modified Quota (in Bytes)
   */
  private Long quota;

  @Setter(NONE)
  private List<String> groups = new ArrayList<>();

  @Builder
  private OwncloudModificationUser(
      String username,
      String password,
      boolean enabled,
      String displayname,
      String email,
      Long quota,
      @Singular("group") List<String> groups) {
    this.username = username;
    setPassword(password);
    setEnabled(enabled);
    setDisplayname(displayname);
    setEmail(email);
    setQuota(quota);
    this.groups.addAll(groups);
  }

  /**
   * Creates a new Instance by copying the Values of an existing {@link OwncloudUserDetails} Object.
   * <p/>
   * A {@link OwncloudUserDetails} Object will be returned by the
   * <code>OwncloudUserDetailsService.loadUserByUsername(String)</code> during
   * the Authentication Process of the
   * <code>OwncloudAuthenticationProvider.authenticate(org.springframework.security.core.Authentication)</code>
   * and resist as a {@link Principal} within the
   * {@link org.springframework.security.core.Authentication} Object (returned by
   * {@link org.springframework.security.core.Authentication#getPrincipal()}
   *
   * @param userDetails
   *            existing {@link OwncloudUserDetails} Object
   * @return new modifiable OwncloudUser Object
   */
  public static OwncloudModificationUser of(OwncloudUserDetails userDetails) {
    Validate.notNull(userDetails);

    OwncloudModificationUser modificationUser =
        OwncloudModificationUser.builder()
                                .username(userDetails.getUsername())
                                .password(userDetails.getPassword())
                                .enabled(userDetails.isEnabled())
                                .displayname(userDetails.getDisplayname())
                                .email(userDetails.getEmail())
                                .quota(userDetails.getQuota())
                                .build();
    Optional.of(userDetails)
            .map(OwncloudUserDetails::getAuthorities)
            .orElseGet(ArrayList::new)
            .stream()
            .map(GrantedAuthority::getAuthority)
            .forEach(modificationUser.groups::add);
    return modificationUser;
  }

  /**
   * Add a Group to the User
   * @param group Group to be added
   */
  public void addGroup(String group) {
    Optional.ofNullable(group)
            .ifPresent(groups::add);
  }

  /**
   * Remove a Group from the User
   * @param group Group to be removed
   */
  public void removeGroup(String group) {
    Optional.of(group)
            .ifPresent(groups::remove);
  }
}
