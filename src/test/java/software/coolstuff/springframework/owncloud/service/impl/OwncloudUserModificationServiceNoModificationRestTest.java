package software.coolstuff.springframework.owncloud.service.impl;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService;

@RestClientTest(OwncloudUserModificationService.class)
@ActiveProfiles("NO-MODIFICATION-URL-TEST")
public class OwncloudUserModificationServiceNoModificationRestTest extends AbstractOwncloudServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserModificationService userModificationService;

  @Override
  public final AbstractOwncloudServiceImpl owncloudService() {
    return (OwncloudUserModificationServiceImpl) userModificationService;
  }

  @Override
  public String getBasicAuthorizationHeader() {
    return null;
  }

  @Override
  protected final String getResourcePrefix() {
    return "/modificationService";
  }

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
