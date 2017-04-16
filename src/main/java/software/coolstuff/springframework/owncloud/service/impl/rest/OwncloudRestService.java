package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.springframework.web.client.RestTemplate;

/**
 * Interface just for make {@link RestTemplate} available within Tests.
 * <p/>
 * Because of AOP we can&apos;t use the abstract Class {@link org.springframework.web.client.support.RestGatewaySupport}
 *
 * @author mufasa1976
 * @since 1.2.0
 */
public interface OwncloudRestService {

  /**
   * get the Rest Template of the Rest Service
   *
   * @return configured Rest Template
   */
  RestTemplate getRestTemplate();

}
