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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/*
ignored by JavaDoc - must be public because of JDK-Proxy and CGLIB-Proxy behaviour
 */
@lombok.Data
public abstract class Ocs {

  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @lombok.Data
  @Builder
  public static class Meta {
    private String status;
    private int statuscode;
    private String message;
  }

  private Meta meta;

  @NoArgsConstructor
  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @XmlRootElement(name = "ocs")
  public static class Void extends Ocs {
    private String data;

    @Builder
    private Void(Meta meta) {
      this.setMeta(meta);
    }
  }

  @NoArgsConstructor
  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @XmlRootElement(name = "ocs")
  public static class Users extends Ocs {

    @NoArgsConstructor
    @lombok.Data
    public static class Data {

      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PACKAGE)
      @lombok.Data
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

  @NoArgsConstructor
  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @XmlRootElement(name = "ocs")
  public static class User extends Ocs {

    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @lombok.Data
    @Builder
    public static class Data {

      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PRIVATE)
      @lombok.Data
      @Builder
      public static class Quota {
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

  @NoArgsConstructor
  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @XmlRootElement(name = "ocs")
  public static class Groups extends Ocs {

    @NoArgsConstructor
    @lombok.Data
    public static class Data {

      @NoArgsConstructor
      @AllArgsConstructor(access = AccessLevel.PACKAGE)
      @lombok.Data
      @Builder
      public static class Group {
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
