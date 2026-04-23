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
package org.smallmind.nutsnbolts.lang;

/**
 * Static helpers that render a {@link Throwable} and its cause chain into either a string or a
 * line array, collapsing repeated frames across causes in the same style as the JDK.
 */
public class StackTraceUtility {

  /**
   * Renders the full stack trace of the given throwable, including its cause chain, into an array
   * of individual text lines.
   *
   * @param throwable the throwable to render
   * @return an array of lines representing the stack trace
   */
  public static String[] obtainStackTraceAsArray (Throwable throwable) {

    return obtainStackTrace(new ArrayStackTraceAccumulator(), throwable).asArray();
  }

  /**
   * Renders the full stack trace of the given throwable, including its cause chain, into a single
   * string with each line separated by the platform line separator.
   *
   * @param throwable the throwable to render
   * @return the stack trace as a multi-line string
   */
  public static String obtainStackTraceAsString (Throwable throwable) {

    return obtainStackTrace(new StringStackTraceAccumulator(), throwable).toString();
  }

  /**
   * Renders the throwable and its full cause chain into the given accumulator, collapsing frames
   * that repeat from the enclosing cause with a "{@code ... N more}" summary.
   *
   * @param accumulator the accumulator that receives each rendered line
   * @param throwable   the throwable whose trace is to be rendered
   * @param <S>         the concrete accumulator type
   * @return the same accumulator after all lines have been appended
   */
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

  /**
   * Searches {@code prevStackTrace} for {@code singleElement} and, if found, returns the number of
   * frames that remain from that position to the end of the previous trace; returns {@code -1} if
   * no match is found.
   *
   * @param singleElement  the frame from the current throwable being checked
   * @param prevStackTrace the stack trace of the enclosing cause
   * @return the count of repeated trailing frames, or {@code -1} if this element is not repeated
   */
  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {

        return prevStackTrace.length - count;
      }
    }

    return -1;
  }
}
