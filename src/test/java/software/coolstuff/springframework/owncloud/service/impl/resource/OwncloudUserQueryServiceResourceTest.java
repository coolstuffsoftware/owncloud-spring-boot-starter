package software.coolstuff.springframework.owncloud.service.impl.resource;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.AbstractOwncloudTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

@RestClientTest(OwncloudUserQueryService.class)
@ActiveProfiles("RESOURCE-TEST")
public class OwncloudUserQueryServiceResourceTest extends AbstractOwncloudTest {

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  protected final String getResourcePrefix() {
    return "/queryService";
  }

  @Test
  public void testFindAllGroups() {
    List<String> groups = userQueryService.findAllGroups();
    Assert.assertTrue(CollectionUtils.isEqualCollection(groups, Lists.newArrayList("Group1", "Group2")));
  }
}
