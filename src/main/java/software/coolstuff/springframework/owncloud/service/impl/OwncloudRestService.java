package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.web.client.RestTemplate;

public interface OwncloudRestService {

  RestTemplate getRestTemplate();

}
