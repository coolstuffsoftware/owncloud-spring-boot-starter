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
package software.coolstuff.springframework.owncloud.service.impl.local;

import lombok.*;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.http.MediaType;
import software.coolstuff.springframework.owncloud.model.OwncloudResource;

import java.net.URI;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@ToString
class OwncloudLocalResourceImpl implements OwncloudLocalResourceExtension {

  private URI href;
  @Setter(AccessLevel.PUBLIC)
  private String name;
  private LocalDateTime lastModifiedAt;
  private MediaType mediaType;
  private String eTag;

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(eTag)
        .append(href)
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !ClassUtils.isAssignable(obj.getClass(), OwncloudResource.class)) {
      return false;
    }
    OwncloudResource otherObj = (OwncloudResource) obj;
    return new EqualsBuilder()
        .append(eTag, otherObj.getETag())
        .append(href, otherObj.getHref())
        .isEquals();
  }

}
