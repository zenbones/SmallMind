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
package org.smallmind.nutsnbolts.util;

/**
 * Utility methods for converting arbitrary strings into valid Java enum constant names in upper snake-case.
 */
public class EnumUtility {

  private enum LetterState {NONE, DIGIT, UPPER_LETTER, LOWER_LETTER, WHITESPACE, OTHER}

  /**
   * Converts the given string into an upper snake-case enum name, separating transitions into digit sequences
   * with underscores by default.
   *
   * @param anyCase the source text to convert
   * @return an upper snake-case string suitable for use as an enum constant name
   */
  public static String toEnumName (String anyCase) {

    return toEnumName(anyCase, true);
  }

  /**
   * Converts the given string into an upper snake-case enum name, with optional underscore insertion before digit runs.
   *
   * @param anyCase      the source text to convert
   * @param prefixDigits {@code true} to insert an underscore before transitions into digit sequences; {@code false} to omit it
   * @return an upper snake-case string suitable for use as an enum constant name
   */
  public static String toEnumName (String anyCase, boolean prefixDigits) {

    StringBuilder fieldBuilder = new StringBuilder();
    LetterState prevState = LetterState.NONE;
    int stateCount = 0;
    boolean separatorPending = false;

    for (int count = 0; count < anyCase.length(); count++) {

      char currentChar = anyCase.charAt(count);
      LetterState state;

      if (Character.isWhitespace(currentChar)) {
        state = LetterState.WHITESPACE;
      } else if (Character.isDigit(currentChar)) {
        state = LetterState.DIGIT;
      } else if (Character.isLetter(currentChar)) {
        state = Character.isUpperCase(currentChar) ? LetterState.UPPER_LETTER : LetterState.LOWER_LETTER;
      } else {
        state = LetterState.OTHER;
      }

      if (!state.equals(LetterState.WHITESPACE)) {
        // Queue a separator on every transition that would have demanded one, instead of emitting it eagerly.
        // A trailing run of OTHER characters leaves the flag set and is never flushed, so no dangling underscore appears.
        if ((count > 0) && (!state.equals(prevState)) && (currentChar != '_') && (!(prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER))) && ((!state.equals(LetterState.DIGIT)) || prefixDigits)) {
          separatorPending = true;
        }

        if (!state.equals(LetterState.OTHER)) {
          if (separatorPending && (fieldBuilder.length() > 0)) {
            fieldBuilder.append('_');
          }
          separatorPending = false;

          if (prevState.equals(LetterState.UPPER_LETTER) && state.equals(LetterState.LOWER_LETTER) && stateCount > 0) {
            fieldBuilder.insert(fieldBuilder.length() - 1, '_');
          }

          fieldBuilder.append(state.equals(LetterState.LOWER_LETTER) ? Character.toUpperCase(currentChar) : currentChar);
        }
      }

      stateCount = prevState.equals(state) ? stateCount + 1 : 0;
      prevState = state;
    }

    return fieldBuilder.toString();
  }
}
