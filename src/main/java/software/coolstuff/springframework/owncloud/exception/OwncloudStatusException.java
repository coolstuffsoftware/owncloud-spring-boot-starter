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
package software.coolstuff.springframework.owncloud.exception;

/**
 * This is the abstract Exception for the mapping between an Owncloud User Provisioning API Response Status
 * and any concrete Java Exception
 *
 * @author mufasa1976
 * @since 1.0.0
 */
public abstract class OwncloudStatusException extends RuntimeException {

  private static final long serialVersionUID = -3354624827447206574L;

  public OwncloudStatusException(String msg) {
    super(msg);
  }

  public OwncloudStatusException(String msg, Throwable cause) {
    super(msg, cause);
  }

  public OwncloudStatusException(Throwable cause) {
    super(cause);
  }

}
