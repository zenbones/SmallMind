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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class NumberComparator implements Comparator<Number> {

  @Override
  public int compare (Number number1, Number number2) {

    if (number1 == null) {
      return (number2 == null) ? 0 : -1;
    } else if (number2 == null) {
      return 1;
    } else if (number1 instanceof BigDecimal) {
      if (number2 instanceof BigDecimal) {
        return ((BigDecimal)number1).compareTo((BigDecimal)number2);
      } else if (number2 instanceof BigInteger) {
        return ((BigDecimal)number1).compareTo(new BigDecimal((BigInteger)number2));
      } else {

        Class<? extends Number> class2 = number2.getClass();

        if ((Double.class.equals(class2)) || (double.class.equals(class2)) || (Float.class.equals(class2)) || (float.class.equals(class2))) {
          return ((BigDecimal)number1).compareTo(BigDecimal.valueOf(number2.doubleValue()));
        } else if ((Long.class.equals(class2)) || (long.class.equals(class2)) || (Integer.class.equals(class2)) || (int.class.equals(class2)) || (Short.class.equals(class2)) || (short.class.equals(class2)) || (Byte.class.equals(class2)) || (byte.class.equals(class2))) {
          return ((BigDecimal)number1).compareTo(BigDecimal.valueOf(number2.longValue()));
        } else {
          throw new FormattedRuntimeException("Unable to compare non-comparable unknown number type(%s)", number2.getClass());
        }
      }
    } else if (number1 instanceof BigInteger) {
      if (number2 instanceof BigDecimal) {
        return new BigDecimal((BigInteger)number1).compareTo((BigDecimal)number2);
      } else if (number2 instanceof BigInteger) {
        return ((BigInteger)number1).compareTo((BigInteger)number2);
      } else {

        Class<? extends Number> class2 = number2.getClass();

        if ((Double.class.equals(class2)) || (double.class.equals(class2)) || (Float.class.equals(class2)) || (float.class.equals(class2))) {
          return new BigDecimal((BigInteger)number1).compareTo(BigDecimal.valueOf(number2.doubleValue()));
        } else if ((Long.class.equals(class2)) || (long.class.equals(class2)) || (Integer.class.equals(class2)) || (int.class.equals(class2)) || (Short.class.equals(class2)) || (short.class.equals(class2)) || (Byte.class.equals(class2)) || (byte.class.equals(class2))) {
          return ((BigInteger)number1).compareTo(BigInteger.valueOf(number2.longValue()));
        } else {
          throw new FormattedRuntimeException("Unable to compare non-comparable unknown number type(%s)", number2.getClass());
        }
      }
    } else {

      Class<? extends Number> class1 = number1.getClass();

      if ((Long.class.equals(class1)) || (long.class.equals(class1)) || (Integer.class.equals(class1)) || (int.class.equals(class1)) || (Short.class.equals(class1)) || (short.class.equals(class1)) || (Byte.class.equals(class1)) || (byte.class.equals(class1))) {
        if (number2 instanceof BigDecimal) {
          return ((BigDecimal)number2).compareTo(BigDecimal.valueOf(number1.longValue())) * -1;
        } else if (number2 instanceof BigInteger) {
          return ((BigInteger)number2).compareTo(BigInteger.valueOf(number1.longValue())) * -1;
        } else {

          Class<? extends Number> class2 = number2.getClass();

          if ((Double.class.equals(class2)) || (double.class.equals(class2)) || (Float.class.equals(class2)) || (float.class.equals(class2))) {
            return Double.compare(number1.doubleValue(), number2.doubleValue());
          } else if ((Long.class.equals(class2)) || (long.class.equals(class2)) || (Integer.class.equals(class2)) || (int.class.equals(class2)) || (Short.class.equals(class2)) || (short.class.equals(class2)) || (Byte.class.equals(class2)) || (byte.class.equals(class2))) {
            return Long.compare(number1.longValue(), number2.longValue());
          } else {
            throw new FormattedRuntimeException("Unable to compare non-comparable unknown number type(%s)", number2.getClass());
          }
        }
      } else if ((Double.class.equals(class1)) || (double.class.equals(class1)) || (Float.class.equals(class1)) || (float.class.equals(class1))) {
        if (number2 instanceof BigDecimal) {
          return ((BigDecimal)number2).compareTo(BigDecimal.valueOf(number1.doubleValue())) * -1;
        } else if (number2 instanceof BigInteger) {
          return new BigDecimal((BigInteger)number2).compareTo(BigDecimal.valueOf(number1.doubleValue())) * -1;
        } else {

          Class<? extends Number> class2 = number2.getClass();

          if ((Double.class.equals(class2)) || (double.class.equals(class2)) || (Float.class.equals(class2)) || (float.class.equals(class2)) || (Long.class.equals(class2)) || (long.class.equals(class2)) || (Integer.class.equals(class2)) || (int.class.equals(class2)) || (Short.class.equals(class2)) || (short.class.equals(class2)) || (Byte.class.equals(class2)) || (byte.class.equals(class2))) {
            return Double.compare(number1.doubleValue(), number2.doubleValue());
          } else {
            throw new FormattedRuntimeException("Unable to compare non-comparable unknown number type(%s)", number2.getClass());
          }
        }
      } else {
        throw new FormattedRuntimeException("Unable to compare non-comparable unknown number type(%s)", number1.getClass());
      }
    }
  }
}
