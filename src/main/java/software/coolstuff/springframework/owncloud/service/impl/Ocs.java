package software.coolstuff.springframework.owncloud.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

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
      static class Element {
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

      private boolean enabled;
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
