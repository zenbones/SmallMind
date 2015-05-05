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
package org.smallmind.nutsnbolts.http;

import java.text.StringCharacterIterator;

public class HexCodec {

  static final String validHex = "1234567890ABCDEFabcdef";

  public static String hexDecode (String value)
    throws NumberFormatException {

    StringCharacterIterator valueIter;
    StringBuilder modBuilder = new StringBuilder();
    String hexNum;
    int hexInt;

    valueIter = new StringCharacterIterator(value);
    while (valueIter.current() != StringCharacterIterator.DONE) {
      if (valueIter.current() == '+') {
        modBuilder.append(' ');
      }
      else if (valueIter.current() != '%') {
        modBuilder.append(valueIter.current());
      }
      else {
        hexNum = "";
        valueIter.next();
        if (validHex.indexOf(valueIter.current()) >= 0) {
          hexNum += valueIter.current();
          valueIter.next();
          if (validHex.indexOf(valueIter.current()) >= 0) {
            hexNum += valueIter.current();
            hexInt = Integer.valueOf(hexNum, 16);
            modBuilder.append((char)hexInt);
          }
          else {
            modBuilder.append('%');
            modBuilder.append(hexNum);
            modBuilder.append(valueIter.current());
          }
        }
        else {
          modBuilder.append('%');
          modBuilder.append(valueIter.current());
        }
      }
      valueIter.next();
    }
    return modBuilder.toString();
  }

  public static String hexEncode (String value) {

    StringCharacterIterator valueIter;
    StringBuilder modBuilder = new StringBuilder();

    valueIter = new StringCharacterIterator(value);
    while (valueIter.current() != StringCharacterIterator.DONE) {
      if (Character.isSpaceChar(valueIter.current())) {
        modBuilder.append('+');
      }
      else if (Character.isLetterOrDigit(valueIter.current())) {
        modBuilder.append(valueIter.current());
      }
      else {
        modBuilder.append('%');
        modBuilder.append(Integer.toHexString((int)valueIter.current()));
      }
      valueIter.next();
    }
    return modBuilder.toString();
  }
}
