package software.coolstuff.springframework.owncloud.service.impl.resource.file;

import org.springframework.test.context.ActiveProfiles;

import software.coolstuff.springframework.owncloud.config.OwncloudResourceFileTest;
import software.coolstuff.springframework.owncloud.service.impl.resource.AbstractOwncloudAuthenticationProviderResourceTest;

@ActiveProfiles("RESOURCE-FILE-TEST")
public class OwncloudAuthenticationProviderResourceFileTest extends AbstractOwncloudAuthenticationProviderResourceTest implements OwncloudResourceFileTest {

}
