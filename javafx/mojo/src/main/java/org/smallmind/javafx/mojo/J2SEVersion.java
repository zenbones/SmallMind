/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.mojo;

public enum J2SEVersion {

  ONE_POINT_SIX_PLUS("1.6+", "http://java.sun.com/products/autodl/j2se");

  private String code;
  private String location;

  private J2SEVersion (String code, String location) {

    this.code = code;
    this.location = location;
  }

  public String getCode () {

    return code;
  }

  public String getLocation () {

    return location;
  }

  public static J2SEVersion fromCode (String code) {

    for (J2SEVersion runtimeVersion : J2SEVersion.values()) {
      if (runtimeVersion.getCode().equals(code)) {

        return runtimeVersion;
      }
    }

    throw new IllegalArgumentException(code);
  }

  public static String[] getValidCodes () {

    String[] validCodes = new String[J2SEVersion.values().length];
    int index = 0;

    for (J2SEVersion runtimeVersion : J2SEVersion.values()) {
      validCodes[index++] = runtimeVersion.getCode();
    }

    return validCodes;
  }
}
