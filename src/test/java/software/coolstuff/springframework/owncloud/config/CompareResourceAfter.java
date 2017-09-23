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
package software.coolstuff.springframework.owncloud.config;

import java.lang.annotation.*;

import software.coolstuff.springframework.owncloud.service.impl.local.file.OwncloudLocalFileTest;

/**
 * By setting this Annotation on a Method it will be invoked by <code>AbstractOwncloudResourceTest.tearDown()</code> right after the TestMethod configured by {@link #value()} and after the changed
 * Resource has been written to the Disk.
 *
 * Plesase note that the Test-Class must also implement the Interface {@link OwncloudLocalFileTest}
 *
 * @author mufasa1976
 * @see OwncloudLocalFileTest
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CompareResourceAfter {

  /**
   * @return Method Name after this Comparsion of the Input Resource with the written Resource should be started
   */
  String value() default "";

}
