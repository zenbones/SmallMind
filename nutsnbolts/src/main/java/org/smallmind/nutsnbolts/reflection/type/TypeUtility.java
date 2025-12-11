/*
 * Copyright (c) 2007 through 2026 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.reflection.type;

public class TypeUtility {

  private static final Class[] PRIMITIVES = new Class[] {Long.class, Boolean.class, Integer.class, Double.class, Float.class, Character.class, Short.class, Byte.class};

  public static boolean isEssentiallyPrimitive (Class<?> aClass) {

    if (!aClass.isPrimitive()) {
      for (Class<?> primitive : PRIMITIVES) {
        if (primitive.equals(aClass)) {

          return true;
        }
      }

      return false;
    }

    return true;
  }

  public static boolean isEssentiallyTheSameAs (Class<?> expectedClass, Class<?> actualClass) {

    if (actualClass.isPrimitive() || expectedClass.isPrimitive()) {
      if (long.class.equals(actualClass) || (Long.class.equals(actualClass))) {

        return long.class.equals(expectedClass) || Long.class.equals(expectedClass);
      }
      if (boolean.class.equals(actualClass) || (Boolean.class.equals(actualClass))) {

        return boolean.class.equals(expectedClass) || Boolean.class.equals(expectedClass);
      }
      if (int.class.equals(actualClass) || (Integer.class.equals(actualClass))) {

        return int.class.equals(expectedClass) || Integer.class.equals(expectedClass);
      }
      if (double.class.equals(actualClass) || (Double.class.equals(actualClass))) {

        return double.class.equals(expectedClass) || Double.class.equals(expectedClass);
      }
      if (float.class.equals(actualClass) || (Float.class.equals(actualClass))) {

        return float.class.equals(expectedClass) || Float.class.equals(expectedClass);
      }
      if (char.class.equals(actualClass) || (Character.class.equals(actualClass))) {

        return char.class.equals(expectedClass) || Character.class.equals(expectedClass);
      }
      if (short.class.equals(actualClass) || (Short.class.equals(actualClass))) {

        return short.class.equals(expectedClass) || Short.class.equals(expectedClass);
      }
      if (byte.class.equals(actualClass) || (Byte.class.equals(actualClass))) {

        return byte.class.equals(expectedClass) || Byte.class.equals(expectedClass);
      }
    }

    return expectedClass.isAssignableFrom(actualClass);
  }

  public static Object getDefaultValue (Class actualClass) {

    if (isEssentiallyPrimitive(actualClass)) {
      if (long.class.equals(actualClass) || (Long.class.equals(actualClass))) {

        return 0L;
      }
      if (boolean.class.equals(actualClass) || (Boolean.class.equals(actualClass))) {

        return false;
      }
      if (int.class.equals(actualClass) || (Integer.class.equals(actualClass))) {

        return 0;
      }
      if (double.class.equals(actualClass) || (Double.class.equals(actualClass))) {

        return 0.0D;
      }
      if (float.class.equals(actualClass) || (Float.class.equals(actualClass))) {

        return 0.0F;
      }
      if (char.class.equals(actualClass) || (Character.class.equals(actualClass))) {

        return (char)0;
      }
      if (short.class.equals(actualClass) || (Short.class.equals(actualClass))) {

        return 0;
      }
      if (byte.class.equals(actualClass) || (Byte.class.equals(actualClass))) {

        return 0;
      }
    }

    return null;
  }
}
