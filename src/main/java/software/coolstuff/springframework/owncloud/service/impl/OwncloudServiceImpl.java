package software.coolstuff.springframework.owncloud.service.impl;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import software.coolstuff.springframework.owncloud.service.api.OwncloudService;

public class OwncloudServiceImpl extends AbstractOwncloudServiceImpl implements OwncloudService {

  public OwncloudServiceImpl(RestTemplateBuilder builder, AbstractOwncloudServiceImpl.Properties properties, MappingJackson2XmlHttpMessageConverter messageConverter) {
    super(builder, properties, messageConverter);
  }

}
