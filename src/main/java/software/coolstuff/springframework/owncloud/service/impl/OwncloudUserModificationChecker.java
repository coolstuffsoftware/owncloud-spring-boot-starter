package software.coolstuff.springframework.owncloud.service.impl;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Aspect
class OwncloudUserModificationChecker {

  private final OwncloudProperties owncloudProperties;

  @Before("execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService.*(..))")
  void checkModificationsAllowed() {
    if (!owncloudProperties.isEnableModifications()) {
      throw new AccessDeniedException("no modifications allowed");
    }
  }
}
