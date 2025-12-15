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
 * Iterates over a JVM method descriptor string and yields parameter type descriptors one at a time.
 */
public class ParameterIterable implements Iterable<String> {

  private final String encrypted;

  /**
   * @param encrypted the substring of a method descriptor containing only parameter descriptors
   */
  public ParameterIterable (String encrypted) {

    this.encrypted = encrypted;
  }

  /**
   * @return a new iterator that walks the descriptor
   */
  public Iterator<String> iterator () {

    return new ParameterIterator();
  }

  /**
   * Iterator that parses the parameter descriptor incrementally.
   */
  private class ParameterIterator implements Iterator<String> {

    private int index = 0;

    /**
     * @return {@code true} if additional parameter descriptors remain
     */
    public boolean hasNext () {

      return index < encrypted.length();
    }

    /**
     * Parses and returns the next parameter descriptor.
     *
     * @return the next descriptor (e.g., {@code I}, {@code [Ljava/lang/String;})
     * @throws NoSuchElementException        if no parameters remain
     * @throws ByteCodeManipulationException if the descriptor contains unsupported syntax
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
     * Builds an array descriptor given the base type and depth.
     *
     * @param baseType   the primitive or object descriptor
     * @param arrayDepth the number of array dimensions
     * @return the fully formed descriptor
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
     * Removal is not supported.
     *
     * @throws UnsupportedOperationException always
     */
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
