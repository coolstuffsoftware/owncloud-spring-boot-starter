package software.coolstuff.springframework.owncloud.service.impl.resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;
import software.coolstuff.springframework.owncloud.service.impl.AbstractOwncloudServiceTest;

@RestClientTest(OwncloudUserModificationService.class)
@ActiveProfiles("NO-MODIFICATION-RESOURCE")
public class OwncloudUserModificationServiceNoModificationResourceTest extends AbstractOwncloudServiceTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Test(expected = AccessDeniedException.class)
  public void testSaveUser() {
    userModificationService.saveUser(new OwncloudModificationUser());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateGroup() {
    userModificationService.createGroup("shouldBeAccessDenied");
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteGroup() {
    userModificationService.deleteGroup("shouldBeAccessDenied");
  }

  @Test(expected = AccessDeniedException.class)
  public void testDeleteUser() {
    userModificationService.deleteUser("shouldBeAccessDenied");
  }

}
