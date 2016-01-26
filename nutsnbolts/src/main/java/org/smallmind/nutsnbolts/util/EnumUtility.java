/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

public class EnumUtility {

  public static String toEnumName (String anyCase) {

    StringBuilder fieldBuilder = new StringBuilder();
    LetterState prevState = LetterState.NONE;
    int stateCount = 0;

    for (int count = 0; count < anyCase.length(); count++) {

      LetterState state;

      if (Character.isWhitespace(anyCase.charAt(count))) {
        state = LetterState.WHITESPACE;
      } else if (Character.isDigit(anyCase.charAt(count))) {
        state = LetterState.DIGIT;
      } else if (Character.isLetter(anyCase.charAt(count))) {
        state = Character.isUpperCase(anyCase.charAt(count)) ? LetterState.UPPER_LETTER : LetterState.LOWER_LETTER;
      } else {
        state = LetterState.OTHER;
      }

      if (!(state.equals(LetterState.WHITESPACE) || ((count > 0) && (anyCase.charAt(count) == '_') && (fieldBuilder.charAt(fieldBuilder.length() - 1) == '_')))) {
        if ((count > 0) && (!state.equals(prevState)) && (anyCase.charAt(count) != '_') && (fieldBuilder.charAt(fieldBuilder.length() - 1) != '_') && (!(prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER)))) {
          fieldBuilder.append('_');
        }
        if (!state.equals(LetterState.OTHER)) {
          if (prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER) && stateCount > 0) {
            fieldBuilder.insert(fieldBuilder.length() - 1, '_');
          }

          fieldBuilder.append(state.equals(LetterState.LOWER_LETTER) ? Character.toUpperCase(anyCase.charAt(count)) : anyCase.charAt(count));
        }
      }

      stateCount = prevState.equals(state) ? stateCount + 1 : 0;
      prevState = state;
    }

    return fieldBuilder.toString();
  }

  private static enum LetterState {NONE, DIGIT, UPPER_LETTER, LOWER_LETTER, WHITESPACE, OTHER}
}