package software.coolstuff.springframework.owncloud.service.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;

@RequiredArgsConstructor
@Aspect
@Slf4j
class OwncloudAuthenticationTypeChecker {

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

  @Before("execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService.*(..)) ||"
      + " execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudUserModificationService.*(..)) ||"
      + " execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService.*(..))")
  void checkAuthenticationObject(JoinPoint joinPoint) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isInvalidAuthentication(authentication)) {
      log.warn("Access denied for Method {}.{}(); because User is not authenticated by the Owncloud Authentication Provider",
          joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
      throw new OwncloudInvalidAuthenticationObjectException(authentication);
    }
  }

  private boolean isInvalidAuthentication(Authentication authentication) {
    return authentication == null || OwncloudUtils.isAuthenticationClassNotSupported(authentication.getClass());
  }

}
