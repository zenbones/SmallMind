/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.nutsnbolts.lang;

public class StackTraceUtility {

  public static String[] obtainStackTraceAsArray (Throwable throwable) {

    return obtainStackTrace(new ArrayStackTraceAccumulator(), throwable).asArray();
  }

  public static String obtainStackTraceAsString (Throwable throwable) {

    return obtainStackTrace(new StringStackTraceAccumulator(), throwable).toString();
  }

  private static <S extends StackTraceAccumulator> S obtainStackTrace (S accumulator, Throwable throwable) {

    StackTraceElement[] prevStackTrace = null;
    StringBuilder lineBuilder;
    int repeatedElements;

    lineBuilder = new StringBuilder();
    do {
      lineBuilder.append(prevStackTrace == null ? "Exception in thread " : "Caused by: ");

      lineBuilder.append(throwable.getClass().getCanonicalName());
      lineBuilder.append(": ");
      lineBuilder.append(throwable.getMessage());
      accumulator.append(lineBuilder);
      lineBuilder.delete(0, lineBuilder.length());

      for (StackTraceElement singleElement : throwable.getStackTrace()) {
        if (prevStackTrace != null) {
          if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
            lineBuilder.append("   ... ");
            lineBuilder.append(repeatedElements);
            lineBuilder.append(" more");
            accumulator.append(lineBuilder);
            lineBuilder.delete(0, lineBuilder.length());
            break;
          }
        }

        lineBuilder.append("   at ");
        lineBuilder.append(singleElement);
        accumulator.append(lineBuilder);
        lineBuilder.delete(0, lineBuilder.length());
      }

      prevStackTrace = throwable.getStackTrace();
    } while ((throwable = throwable.getCause()) != null);

    return accumulator;
  }

  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {

        return prevStackTrace.length - count;
      }
    }

    return -1;
  }
}
