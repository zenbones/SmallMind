/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MutationUtility {

  public static <T, U> U[] toArray (T[] array, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    return toArray(Arrays.asList(array), outType, mutation);
  }

  public static <T, U> U[] toArray (Collection<T> collection, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    if (collection == null) {

      return null;
    } else {
      try {

        U[] outArray = (U[])Array.newInstance(outType, collection.size());
        int index = 0;

        for (T inType : collection) {
          outArray[index++] = mutation.apply(inType);
        }

        return outArray;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }

  public static <T, U> List<U> toList (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toList(Arrays.asList(array), mutation);
  }

  public static <T, U> List<U> toList (Collection<T> collection, Mutation<? super T, U> mutation)
    throws MutationException {

    if (collection == null) {

      return null;
    } else {
      try {

        LinkedList<U> outList = new LinkedList<>();

        for (T inType : collection) {
          outList.add(mutation.apply(inType));
        }

        return outList;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }
}
