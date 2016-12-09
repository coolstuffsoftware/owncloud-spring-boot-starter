package software.coolstuff.springframework.owncloud.config;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class VelocityConfiguration {

  @Bean
  public VelocityEngine velocityEngine() {
    VelocityEngine velocityEngine = new VelocityEngine();

    velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    velocityEngine.setProperty("classpath.resource.loader.description", "Classpath Loader of the Velocity Engine");
    velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());

    velocityEngine.init();
    return velocityEngine;
  }
}
