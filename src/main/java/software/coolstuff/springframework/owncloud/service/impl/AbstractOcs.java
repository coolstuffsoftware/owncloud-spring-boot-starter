package software.coolstuff.springframework.owncloud.service.impl;

import lombok.Data;

@Data
class AbstractOcs {

  @Data
  static class Meta {
    private String status;
    private int statuscode;
    private String message;
  }

  private Meta meta;

}
