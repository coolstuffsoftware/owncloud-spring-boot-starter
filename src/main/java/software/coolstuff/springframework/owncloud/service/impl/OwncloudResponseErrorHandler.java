package software.coolstuff.springframework.owncloud.service.impl;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.exception.OwncloudUnauthorizedException;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public class OwncloudResponseErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    try {
      HttpStatus statusCode = response.getStatusCode();
      if (HttpStatus.UNAUTHORIZED.compareTo(statusCode) == 0) {
        throw new OwncloudUnauthorizedException(statusCode.getReasonPhrase());
      }
      super.handleError(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

}
