package software.coolstuff.springframework.owncloud.service.impl.resource.file;

import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserModificationServiceTest;

@ActiveProfiles("FILE-RESOURCE")
public class OwncloudUserModificationServiceFileResourceTest extends AbstractOwncloudUserModificationServiceTest implements OwncloudModifyingFileResourceTest {

  @Override
  public String getResourcePrefix() {
    return "/modificationService";
  }

  @CompareResourceAfter("testSaveUser_CreateUser_OK_WithoutGroups")
  public void compareAfterTestSaveUser_CreateUser_OK_WithoutGroups(Resource target) throws Exception {
    compareResources(getResourceOf("owncloud_afterSave_User3"), target);
  }

  @CompareResourceAfter("testSaveUser_CreateUser_OK_WithGroups")
  public void compareAfterTestSaveUser_CreateUser_OK_WithGroups(Resource target) throws Exception {
    compareResources(getResourceOf("owncloud_afterSave_User4"), target);
  }

  @CompareResourceAfter("testSaveUser_UpdateUser_OK_WithoutGroups")
  public void compareAfterTestSaveUser_UpdateUser_OK_WithoutGroups(Resource target) throws Exception {
    compareResources(getResourceOf("owncloud_afterSave_User2"), target);
  }

  @CompareResourceAfter("testSaveUser_UpdateUser_OK_WithGroups")
  public void compareTestSaveUser_UpdateUser_OK_WithGroups(Resource target) throws Exception {
    compareResources(getResourceOf("owncloud_afterSave_User1"), target);
  }

  @CompareResourceAfter("testDeleteUser_OK")
  public void compareTestDeleteUser_OK(Resource target) throws Exception {
    compareResources(getResourceOf("owncloud_afterDelete_User1"), target);
  }
}
