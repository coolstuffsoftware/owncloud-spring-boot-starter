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
package software.coolstuff.springframework.owncloud.service.impl.resource;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 * @author mufasa1976
 */
@lombok.Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder
@JacksonXmlRootElement(localName = "owncloud")
class OwncloudResourceData {

  @lombok.Data
  @AllArgsConstructor
  @JacksonXmlRootElement(localName = "group")
  static class Group {

    @JacksonXmlText
    private String group;

  }

  @lombok.Data
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @lombok.Builder
  @JacksonXmlRootElement(localName = "user")
  static class User {

    @NotNull
    @JacksonXmlProperty(localName = "username")
    private String username;

    @JacksonXmlProperty(localName = "password")
    private String password;

    @JacksonXmlProperty(localName = "enabled")
    private boolean enabled = true;

    @JacksonXmlProperty(localName = "displayname")
    private String displayname;

    @JacksonXmlProperty(localName = "email")
    private String email;

    @Singular
    @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
    @JacksonXmlProperty(localName = "group")
    private List<Group> groups;
  }

  @Singular
  @JacksonXmlElementWrapper(localName = "users", useWrapping = true)
  @JacksonXmlProperty(localName = "user")
  private Collection<User> users;

  @Singular
  @JacksonXmlElementWrapper(localName = "groups", useWrapping = true)
  @JacksonXmlProperty(localName = "group")
  private Collection<Group> groups;
}
