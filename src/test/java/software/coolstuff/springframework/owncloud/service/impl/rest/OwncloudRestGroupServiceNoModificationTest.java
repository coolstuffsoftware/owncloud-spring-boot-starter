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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@AutoConfigureWebClient
@ActiveProfiles("REST-NO-MODIFICATION-USER-SERVICE")
public class OwncloudRestGroupServiceNoModificationTest extends AbstractOwncloudServiceTest implements OwncloudRestServiceTest {

  @Autowired
  private OwncloudGroupService groupService;

  @Override
  public final OwncloudRestService owncloudService() {
    return (OwncloudRestService) groupService;
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user", password = "s3cr3t")
  public void testCreateGroup() {
    groupService.create("shouldBeAccessDenied");
  }

  @Test(expected = AccessDeniedException.class)
  @WithMockUser(username = "user", password = "s3cr3t")
  public void testDeleteGroup() {
    groupService.delete("shouldBeAccessDenied");
  }

}
