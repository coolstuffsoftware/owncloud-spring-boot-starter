package software.coolstuff.springframework.owncloud;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

import software.coolstuff.springframework.owncloud.service.impl.OwncloudAutoConfiguration;

@SpringBootConfiguration
@Import(OwncloudAutoConfiguration.class)
public class TestConfiguration {

}
