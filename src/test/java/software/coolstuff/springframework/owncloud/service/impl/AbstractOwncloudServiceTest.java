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
package software.coolstuff.springframework.owncloud.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.*;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.MultiValueMap;

import lombok.*;
import software.coolstuff.springframework.owncloud.config.AuthorityAppenderConfiguration;
import software.coolstuff.springframework.owncloud.config.AuthorityMapperConfiguration;
import software.coolstuff.springframework.owncloud.config.IgnoreOnComponentScan;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.impl.local.OwncloudLocalFileTestExecutionListener;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestService;
import software.coolstuff.springframework.owncloud.service.impl.rest.OwncloudRestServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    properties = "debug=true",
    classes = {
        VelocityConfiguration.class,
        AuthorityAppenderConfiguration.class,
        AuthorityMapperConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudLocalFileTestExecutionListener.class
})
@ComponentScan(excludeFilters = {
    @Filter(IgnoreOnComponentScan.class)
})
public abstract class AbstractOwncloudServiceTest {

  private final static String DEFAULT_PATH = "/ocs/v1.php";
  private final static String VELOCITY_PATH_PREFIX = "/velocity/";

  private final static Format LONG_FORMAT = new DecimalFormat("###########0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private VelocityEngine velocityEngine;

  private MockRestServiceServer server;

  @Before
  public final void setUp() throws Exception {
    if (this instanceof OwncloudRestServiceTest) {
      server = createServer(((OwncloudRestServiceTest) this).owncloudService());
    }
  }

  protected Format getQuotaFormat() {
    return LONG_FORMAT;
  }

  protected final MockRestServiceServer createServer(OwncloudRestService owncloudService) {
    return MockRestServiceServer.createServer(owncloudService.getRestTemplate());
  }

  protected void verifyServer() {
    if (server != null) {
      server.verify();
    }
  }

  protected final MockRestServiceServer getServer() {
    return server;
  }

  protected void respondUsers(RestRequest request, String... users) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    context.put("users", Arrays.asList(users));

    preparedRequest.andRespond(withSuccess(merge("users.vm", context), MediaType.TEXT_XML));
  }

  private boolean isNoRestTestClass() {
    return !(this instanceof OwncloudRestServiceTest);
  }

  private ResponseActions prepareRestRequest(RestRequest request) throws MalformedURLException {
    MockRestServiceServer server = this.server;
    if (request.getServer() != null) {
      server = request.getServer();
    }
    ResponseActions responseActions = server
        .expect(requestToWithPrefix(request.getUrl()))
        .andExpect(method(request.getMethod()));
    if (StringUtils.isNotBlank(request.getBasicAuthentication())) {
      responseActions.andExpect(header(HttpHeaders.AUTHORIZATION, request.getBasicAuthentication()));
    } else {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      responseActions.andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((authentication.getName() + ":" + authentication.getCredentials()).getBytes())));
    }
    return responseActions;
  }

  private RequestMatcher requestToWithPrefix(String uri) throws MalformedURLException {
    String rootURI = null;
    if (isNoResourceLocation()) {
      URL url = new URL(properties.getLocation());
      rootURI = properties.getLocation();
      if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
        rootURI = URI.create(url.toString() + DEFAULT_PATH).toString();
      }
    }
    return requestTo(rootURI + uri);
  }

  private boolean isNoResourceLocation() {
    return !StringUtils.startsWith(properties.getLocation(), "file:") && !StringUtils.startsWith(properties.getLocation(), "classpath:");
  }

  private void setSuccessMetaInformation(Context context) {
    context.put("status", "ok");
    context.put("statuscode", 100);
  }

  private String merge(String templateName, Context context) throws IOException {
    String prefixedTemplateName = templateName;
    if (!StringUtils.startsWith(templateName, VELOCITY_PATH_PREFIX)) {
      prefixedTemplateName = VELOCITY_PATH_PREFIX + templateName;
      if (StringUtils.startsWith(templateName, "/")) {
        prefixedTemplateName = VELOCITY_PATH_PREFIX + StringUtils.substring(templateName, 1);
      }
    }
    Template template = velocityEngine.getTemplate(prefixedTemplateName);
    try (Writer writer = new StringWriter()) {
      template.merge(context, writer);
      writer.flush();
      return writer.toString();
    }
  }

  protected void respondGroups(RestRequest request, String... groups) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    context.put("groups", Arrays.asList(groups));

    preparedRequest.andRespond(withSuccess(merge("groups.vm", context), request.getResponseType()));
  }

  protected void respondFailure(RestRequest request, int statuscode) throws IOException {
    respondFailure(request, statuscode, null);
  }

  protected void respondFailure(RestRequest request, int statuscode, String message) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setFailureMetaInformation(context, statuscode, message);

    preparedRequest.andRespond(withSuccess(merge("void.vm", context), request.getResponseType()));
  }

  protected void respondHttpStatus(RestRequest request, HttpStatus httpStatus) throws MalformedURLException {
    if (isNoRestTestClass()) {
      return;
    }
    prepareRestRequest(request).andRespond(withStatus(httpStatus));
  }

  protected void respondSuccess(RestRequest request) throws IOException {
    respondSuccess(request, null);
  }

  protected void respondSuccess(RestRequest request, MultiValueMap<String, String> requestBody) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);

    if (requestBody != null) {
      preparedRequest = preparedRequest.andExpect(content().formData(requestBody));
    }
    preparedRequest.andRespond(withSuccess(merge("void.vm", context), MediaType.TEXT_XML));
  }

  private void setFailureMetaInformation(Context context, int statuscode, String message) {
    context.put("status", "failure");
    context.put("statuscode", statuscode);
    context.put("message", message != null ? message : "");
  }

  protected void respondUser(RestRequest request, UserResponse userResponse)
      throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    userResponse.fillContext(context);

    preparedRequest.andRespond(withSuccess(merge("user.vm", context), MediaType.TEXT_XML));
  }

  @Setter
  @Builder
  public static class UserResponse {
    private final static String ENABLED = "enabled";
    private final static String EMAIL = "email";
    private final static String DISPLAY_NAME = "displayname";
    private final static String QUOTA = "quota";
    private final static String USED = "used";
    private final static String FREE = "free";
    private final static String RELATIVE = "relative";

    private final static Format FLOAT_FORMAT = new DecimalFormat("###########0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private boolean enabled;
    private String email;
    private String displayname;
    private Long quota;
    private Long used;
    private Long free;
    private Float relative;

    public static class UserResponseBuilder {
      private boolean enabled = true;
      private Long free = 817_928_385L;
      private Long used = 255_813_439L;
      private Float relative = 23.82F;
    }

    public void fillContext(Context context) {
      context.put(ENABLED, Boolean.toString(enabled));
      context.put(EMAIL, email);
      context.put(DISPLAY_NAME, displayname);
      context.put(QUOTA, quota != null ? LONG_FORMAT.format(quota) : null);
      context.put(USED, used != null ? LONG_FORMAT.format(used) : null);
      context.put(FREE, free != null ? LONG_FORMAT.format(free) : null);
      context.put(RELATIVE, relative != null ? FLOAT_FORMAT.format(relative) : null);
    }
  }

  protected void checkAuthorities(String username, Collection<? extends GrantedAuthority> actual, String... expected) {
    assertThat(actual == null ? 0 : actual.size()).isEqualTo(expected.length);
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (ArrayUtils.isNotEmpty(expected)) {
      for (String authority : expected) {
        authorities.add(new SimpleGrantedAuthority(authority));
      }
    }
    if (owncloudGrantedAuthoritiesMapper != null) {
      assertThat(CollectionUtils.isEqualCollection(actual, owncloudGrantedAuthoritiesMapper.mapAuthorities(username, authorities))).isTrue();
    } else if (grantedAuthoritiesMapper != null) {
      assertThat(CollectionUtils.isEqualCollection(actual, grantedAuthoritiesMapper.mapAuthorities(authorities))).isTrue();
    } else {
      assertThat(CollectionUtils.isEqualCollection(actual, authorities)).isTrue();
    }
  }

  protected final String getSecurityContextBasicAuthorizationHeader() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return "Basic " + Base64.getEncoder().encodeToString((authentication.getName() + ":" + authentication.getCredentials()).getBytes());
  }

  @Data
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  protected static class RestRequest {

    private MockRestServiceServer server;
    @NotNull
    private final HttpMethod method;
    @NotNull
    private final String url;
    private String basicAuthentication;
    private MediaType responseType = MediaType.TEXT_XML;

    public static class RestRequestBuilder {
      private MediaType responseType = MediaType.TEXT_XML;
    }
  }
}
