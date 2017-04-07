/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupAlreadyExistsException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGroupService;

@Slf4j
class OwncloudGroupRestServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudGroupService {

  private static final String GROUP_PATH = "/cloud/groups/{group}";

  OwncloudGroupRestServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public void create(String groupname) {
    Validate.notBlank(groupname);

    log.trace("Create Message Body for Create Request of Group {}", groupname);
    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    log.debug("Create Group {} on Location {}", groupname, getLocation());
    exchange(
        GROUP_PATH,
        HttpMethod.POST,
        multiValuedEntity(data),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkCreateGroup(authenticatedUser, uri, meta, groupname),
        groupname);
    log.info("Group {} successfully created on Location {}", groupname, getLocation());
  }

  private void checkCreateGroup(String authenticatedUser, String uri, Ocs.Meta meta, String groupname) {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    String exceptionMessage;
    switch (meta.getStatuscode()) {
      case 101:
        log.error("Error 101: {}", meta.getMessage());
        throw new IllegalArgumentException(meta.getMessage());
      case 102:
        log.warn("Error 102: Group {} already exists", groupname);
        throw new OwncloudGroupAlreadyExistsException(groupname);
      case 103:
        exceptionMessage = String.format("Failed to create Group %s. Reason: %s", groupname, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error("Error 103: {}", exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

  @Override
  public void delete(String groupname) {
    Validate.notBlank(groupname);

    log.trace("Create Message Body for Delete Request of Group {}", groupname);
    Map<String, List<String>> data = new HashMap<>();
    data.put("groupid", Lists.newArrayList(groupname));

    log.debug("Delete Group {} on Location {}", groupname, getLocation());
    exchange(
        GROUP_PATH,
        HttpMethod.DELETE,
        multiValuedEntity(data),
        Ocs.Void.class,
        (authenticatedUser, uri, meta) -> checkDeleteGroup(authenticatedUser, uri, meta, groupname),
        groupname);
    log.info("Group {} successfully removed from Location {}", groupname, getLocation());
  }

  private void checkDeleteGroup(String authenticatedUser, String uri, Ocs.Meta meta, String groupname) {
    if ("ok".equals(meta.getStatus())) {
      return;
    }

    String exceptionMessage;
    switch (meta.getStatuscode()) {
      case 101:
        log.warn("Error 101: Group {} not exists", groupname);
        throw new OwncloudGroupNotFoundException(groupname);
      case 102:
        exceptionMessage = String.format("Failed to delete Group %s. Reason: %s", groupname, StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error("Error 102: {}", exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
      case 997:
        exceptionMessage = String.format("User %s is not authorized to access Resource %s", authenticatedUser, uri);
        log.warn("Error 997: {}", exceptionMessage);
        throw new AccessDeniedException(exceptionMessage);
      default:
        exceptionMessage = String.format("Unknown Error Code %d. Reason: %s", meta.getStatuscode(), StringUtils.defaultIfEmpty(meta.getMessage(), ""));
        log.error(exceptionMessage);
        throw new IllegalStateException(exceptionMessage);
    }
  }

}
