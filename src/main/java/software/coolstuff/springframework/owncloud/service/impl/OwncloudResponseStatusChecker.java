package software.coolstuff.springframework.owncloud.service.impl;

import software.coolstuff.springframework.owncloud.exception.OwncloudStatusException;

@FunctionalInterface
public interface OwncloudResponseStatusChecker {

  void checkForFailure(String uri, AbstractOcs.Meta metaInformation) throws OwncloudStatusException;

}
