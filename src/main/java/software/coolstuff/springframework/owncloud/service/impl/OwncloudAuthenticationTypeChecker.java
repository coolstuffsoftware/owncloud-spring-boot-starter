/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudInvalidAuthenticationObjectException;

@RequiredArgsConstructor
@Aspect
@Slf4j
public class OwncloudAuthenticationTypeChecker {

  private final OwncloudProperties owncloudProperties;

  @Before("execution(* software.coolstuff.springframework.owncloud.service.api..*.*(..)) && "
      + "@annotation(software.coolstuff.springframework.owncloud.service.impl.CheckOwncloudModification)")
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

  @Before("execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudUserService.*(..)) || "
      + "execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService.*(..)) || "
      + "execution(* software.coolstuff.springframework.owncloud.service.api.OwncloudResourceService.*(..)) || "
      + "execution(* software.coolstuff.springframework.owncloud.service.impl.OwncloudUserDetailsService.*(..))")
  void checkAuthenticationObject(JoinPoint joinPoint) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (OwncloudUtils.isInvalidAuthentication(authentication)) {
      log.warn("Access denied for Method {}.{}(); because User is not authenticated by the Owncloud Authentication Provider",
          joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
      throw new OwncloudInvalidAuthenticationObjectException(authentication, UsernamePasswordAuthenticationToken.class);
    }
  }

}
