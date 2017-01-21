package software.coolstuff.springframework.owncloud.service.impl.rest;

import org.springframework.web.client.RestTemplate;

/**
 * Interface just for make Test available
 *
 * @author mufasa1976
 */
public interface OwncloudRestService {

  /**
   * get the Rest Template of the Rest Service
   *
   * @return configured Rest Template
   */
  RestTemplate getRestTemplate();

}
