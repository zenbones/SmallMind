/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.util;

import java.lang.reflect.Array;
import java.util.Objects;

public class ArrayUtility {

  public static <T> T[] clone (Class<T> clazz, T[] original) {

    if (original == null) {

      return null;
    } else {

      T[] copy = (T[])Array.newInstance(clazz, original.length);

      System.arraycopy(original, 0, copy, 0, original.length);

      return copy;
    }
  }

  public static <T> T[] concatenate (Class<T> clazz, T[] first, T... second) {

    if ((first == null) || (first.length == 0)) {

      return (second == null) ? (T[])Array.newInstance(clazz, 0) : second;
    } else if ((second == null) || (second.length == 0)) {

      return first;
    } else {

      T[] joined = (T[])Array.newInstance(clazz, first.length + second.length);

      System.arraycopy(first, 0, joined, 0, first.length);
      System.arraycopy(second, 0, joined, first.length, second.length);

      return joined;
    }
  }

  public static <T> int linearSearch (T[] array, T key) {

    if ((array != null) && (array.length > 0)) {

      int index;

      for (index = 0; index < array.length; index++) {
        if (Objects.equals(array[index], key)) {

          return index;
        }
      }
    }

    return -1;
  }
}
