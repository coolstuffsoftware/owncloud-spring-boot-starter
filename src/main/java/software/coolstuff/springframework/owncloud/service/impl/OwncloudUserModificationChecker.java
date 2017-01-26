package software.coolstuff.springframework.owncloud.service.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Aspect
@Slf4j
class OwncloudUserModificationChecker {

  private final OwncloudProperties owncloudProperties;

  @Before("execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService.*(..))")
  void checkModificationsAllowed(JoinPoint joinPoint) {
    if (modificationsNotAllowed()) {
      log.warn("Access denied for Method {}.{}() because Modifications are not allowed",
          joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
      throw new AccessDeniedException("no modifications allowed");
    }
  }

  private boolean modificationsNotAllowed() {
    return owncloudProperties.getUserService() == null || !owncloudProperties.getUserService().isEnableModifications();
  }

}
