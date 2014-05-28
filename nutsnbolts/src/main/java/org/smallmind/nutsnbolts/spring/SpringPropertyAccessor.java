/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

  private static PropertyPlaceholderStringValueResolver INSTANCE;

  private Set<String> keySet;

  public static synchronized void setInstance (PropertyPlaceholderStringValueResolver instance) {

    INSTANCE = instance;
  }

  private static synchronized PropertyPlaceholderStringValueResolver getInstance () {

    if (INSTANCE == null) {
      throw new RuntimeBeansException("No instance(%s) has been set - please report this error to your operations team", PropertyPlaceholderStringValueResolver.class.getSimpleName());
    }

    return INSTANCE;
  }

  public SpringPropertyAccessor () {

    keySet = getInstance().getKeySet();
  }

  public Set<String> getKeySet () {

    return keySet;
  }

  public String asString (String key) {

    if (!keySet.contains(key)) {

      return null;
    }

    return getInstance().resolveStringValue("${" + key + "}");
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
