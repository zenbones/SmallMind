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
package org.smallmind.nutsnbolts.reflection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/*
 * boolean Z
 * byte B
 * char C
 * short S
 * int I
 * long J
 * float F
 * double D
 * array [
 * object L;
 * type T;
 */

/**
 * Parses the parameter section of a JVM method descriptor and provides an {@link Iterable} that
 * yields individual parameter type descriptors (e.g. {@code I}, {@code Ljava/lang/String;}).
 */
public class ParameterIterable implements Iterable<String> {

  private final String encrypted;

  /**
   * Constructs an iterable over the parameter portion of a JVM method descriptor.
   *
   * @param encrypted the parameter descriptor string, i.e. the content between the opening and closing
   *                  parentheses of a method descriptor
   */
  public ParameterIterable (String encrypted) {

    this.encrypted = encrypted;
  }

  /**
   * Returns a new iterator positioned at the start of the parameter descriptor string.
   *
   * @return an iterator that yields parameter descriptors one at a time
   */
  public Iterator<String> iterator () {

    return new ParameterIterator();
  }

  /**
   * Stateful iterator that advances through the parameter descriptor string, yielding one descriptor per call.
   */
  private class ParameterIterator implements Iterator<String> {

    private int index = 0;

    /**
     * Returns {@code true} if there are more parameter descriptors to parse.
     *
     * @return {@code true} while the current position has not yet reached the end of the descriptor string
     */
    public boolean hasNext () {

      return index < encrypted.length();
    }

    /**
     * Parses and returns the next parameter type descriptor from the descriptor string.
     *
     * @return the next descriptor token, e.g. {@code I} for {@code int} or {@code Ljava/lang/String;} for {@link String}
     * @throws NoSuchElementException        if the end of the descriptor string has already been reached
     * @throws ByteCodeManipulationException if an unrecognised character is encountered in the descriptor
     */
    public String next () {

      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      int arrayDepth = 0;

      do {
        switch (encrypted.charAt(index++)) {
          case 'Z':
            return assembleType("Z", arrayDepth);
          case 'B':
            return assembleType("B", arrayDepth);
          case 'C':
            return assembleType("C", arrayDepth);
          case 'S':
            return assembleType("S", arrayDepth);
          case 'I':
            return assembleType("I", arrayDepth);
          case 'J':
            return assembleType("J", arrayDepth);
          case 'F':
            return assembleType("F", arrayDepth);
          case 'D':
            return assembleType("D", arrayDepth);
          case 'L':

            StringBuilder objectBuilder = new StringBuilder("L");
            char objectChar;

            do {
              if ((objectChar = encrypted.charAt(index++)) == ';') {

                return assembleType(objectBuilder.append(';').toString(), arrayDepth);
              } else {
                objectBuilder.append(objectChar);
              }
            } while (index < encrypted.length());
            break;
          case '[':
            arrayDepth++;
            break;
          default:
            throw new ByteCodeManipulationException("Unknown format for parameter encrypted(%s)", encrypted);
        }
      } while (index < encrypted.length());

      throw new ByteCodeManipulationException("Unknown format for parameter encrypted(%s)", encrypted);
    }

    /**
     * Prepends the appropriate number of {@code [} characters to a base type descriptor to form an array descriptor.
     *
     * @param baseType   the base primitive or object descriptor, e.g. {@code I} or {@code Ljava/lang/String;}
     * @param arrayDepth the number of array dimensions; zero means a non-array type is returned unchanged
     * @return the fully qualified array descriptor, e.g. {@code [[I} for a two-dimensional int array
     */
    private String assembleType (String baseType, int arrayDepth) {

      if (arrayDepth == 0) {

        return baseType;
      }

      StringBuilder arrayBuilder = new StringBuilder();

      for (int count = 0; count < arrayDepth; count++) {
        arrayBuilder.append('[');
      }
      arrayBuilder.append(baseType);

      return arrayBuilder.toString();
    }

    /**
     * Not supported; parameter descriptors may not be removed from the underlying string.
     *
     * @throws UnsupportedOperationException always, since removal is not supported
     */
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
