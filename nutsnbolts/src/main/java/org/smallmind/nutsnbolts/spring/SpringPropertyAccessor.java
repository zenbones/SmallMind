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
package org.smallmind.nutsnbolts.spring;

import java.util.Set;
import org.smallmind.nutsnbolts.util.None;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.nutsnbolts.util.Some;

public class SpringPropertyAccessor {

  PropertyPlaceholderStringValueResolver stringValueResolver;
  private final Set<String> keySet;

  public SpringPropertyAccessor (PropertyPlaceholderStringValueResolver stringValueResolver) {

    this.stringValueResolver = stringValueResolver;

    keySet = stringValueResolver.getKeySet();
  }

  public Set<String> getKeySet () {

    return keySet;
  }

  public String asString (String key) {

    if (!keySet.contains(key)) {

      return null;
    }

    return stringValueResolver.resolveStringValue("${" + key + "}");
  }

  public Option<Boolean> asBoolean (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return None.none();
    }

    return new Some<Boolean>(Boolean.parseBoolean(stringValue));
  }

  public Option<Long> asLong (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return None.none();
    }

    try {
      return new Some<Long>(Long.parseLong(stringValue));
    }
    catch (NumberFormatException numberFormatException) {
      throw new RuntimeBeansException("The value of key(%s) must interpolate as an long(%s)", key, stringValue);
    }
  }

  public Option<Integer> asInt (String key) {

    String stringValue;

    if ((stringValue = asString(key)) == null) {

      return None.none();
    }

    try {
      return new Some<Integer>(Integer.parseInt(stringValue));
    }
    catch (NumberFormatException numberFormatException) {
      throw new RuntimeBeansException("The value of key(%s) must interpolate as an int(%s)", key, stringValue);
    }
  }
}
