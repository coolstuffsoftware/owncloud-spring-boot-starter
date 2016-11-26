package software.coolstuff.springframework.owncloud;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;

import software.coolstuff.springframework.owncloud.config.OwncloudConfiguration;

@SpringBootConfiguration
@Import(OwncloudConfiguration.class)
public class TestConfiguration {

}
