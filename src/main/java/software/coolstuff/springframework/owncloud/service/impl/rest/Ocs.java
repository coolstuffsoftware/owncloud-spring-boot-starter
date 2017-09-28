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

import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@lombok.Data
abstract class Ocs {

  @lombok.Data
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  static class Meta {
    private String status;
    private int statuscode;
    private String message;
  }

  private Meta meta;

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @NoArgsConstructor
  @XmlRootElement(name = "ocs")
  static class Void extends Ocs {
    private String data;

    @Builder
    private Void(Meta meta) {
      this.setMeta(meta);
    }
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @NoArgsConstructor
  @XmlRootElement(name = "ocs")
  static class Users extends Ocs {

    @lombok.Data
    @NoArgsConstructor
    static class Data {

      @lombok.Data
      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PACKAGE)
      public static class Element {
        private String element;
      }

      private List<Element> users;

      @Builder
      private Data(@Singular("user") List<String> users) {
        List<Element> elements = new ArrayList<>();
        for (String user : users) {
          elements.add(new Element(user));
        }
        this.setUsers(elements);
      }
    }

    private Data data;

    @Builder
    private Users(Meta meta, @Singular("user") List<String> users) {
      setMeta(meta);
      setData(new Data(users));
    }
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @NoArgsConstructor
  @XmlRootElement(name = "ocs")
  static class User extends Ocs {

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    static class Data {

      @lombok.Data
      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @Builder
      static class Quota {
        private Long free;
        private Long used;
        private Long total;
        private Float relative;
      }

      @lombok.Builder.Default
      private boolean enabled = true;
      private Quota quota;
      private String email;
      private String displayname;
    }

    private Data data;

    @Builder
    private User(Meta meta, Data data) {
      setMeta(meta);
      setData(data);
    }
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @NoArgsConstructor
  @XmlRootElement(name = "ocs")
  static class Groups extends Ocs {

    @lombok.Data
    @NoArgsConstructor
    static class Data {

      @lombok.Data
      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PACKAGE)
      @Builder
      static class Group {
        private String group;
      }

      private List<Group> groups;

      @Builder
      private Data(@Singular("group") List<String> groups) {
        List<Group> innerGroups = new ArrayList<>();
        for (String group : groups) {
          innerGroups.add(new Group(group));
        }
        this.setGroups(innerGroups);
      }
    }

    private Data data;

    @Builder
    private Groups(Meta meta, @Singular("group") List<String> groups) {
      setMeta(meta);
      setData(new Data(groups));
    }
  }
}
