package software.coolstuff.springframework.owncloud.service.impl;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
abstract class Ocs {

  @Data
  static class Meta {
    private String status;
    private int statuscode;
    private String message;
  }

  private Meta meta;

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  static class Void extends Ocs {

    private String data;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  static class Users extends Ocs {

    @lombok.Data
    static class Data {

      @lombok.Data
      @AllArgsConstructor
      static class Element {

        private String element;

      }

      private List<Element> users;
    }

    private Data data;
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  static class User extends Ocs {

    @lombok.Data
    static class Data {

      @lombok.Data
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
  }

  @lombok.Data
  @EqualsAndHashCode(callSuper = true)
  @XmlRootElement(name = "ocs")
  static class Groups extends Ocs {

    @lombok.Data
    static class Data {

      @lombok.Data
      @AllArgsConstructor
      static class Group {

        private String group;

      }

      private List<Group> groups;

    }

    private Data data;

  }

}
