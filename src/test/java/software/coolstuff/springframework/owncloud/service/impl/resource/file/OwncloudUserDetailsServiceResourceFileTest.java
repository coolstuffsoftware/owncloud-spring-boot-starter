package software.coolstuff.springframework.owncloud.service.impl.resource.file;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.config.OwncloudResourceFileTest;
import software.coolstuff.springframework.owncloud.service.impl.resource.AbstractOwncloudUserDetailsServiceResourceTest;

@ActiveProfiles("RESOURCE-FILE-TEST")
public class OwncloudUserDetailsServiceResourceFileTest extends AbstractOwncloudUserDetailsServiceResourceTest implements OwncloudResourceFileTest {

}
