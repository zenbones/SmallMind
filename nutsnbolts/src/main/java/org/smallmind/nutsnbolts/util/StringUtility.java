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
 * Utility methods for string case conversion, identifier validation, and miscellaneous string manipulation.
 */
public class StringUtility {

  /**
   * Converts a string to display case, capitalizing the first character of each word and lowercasing all other characters; the source is treated as a single word.
   *
   * @param anyCase the source string to convert
   * @return the display-cased string
   */
  public static String toDisplayCase (String anyCase) {

    return toDisplayCase(anyCase, '\0');
  }

  /**
   * Converts a string to display case, capitalizing the first character of each word (delimited by {@code wordMarker}) and lowercasing the rest; the delimiter character is replaced by a space.
   *
   * @param anyCase    the source string to convert
   * @param wordMarker character that marks word boundaries; occurrences are replaced by spaces in the output
   * @return the display-cased string with separators replaced by spaces
   */
  public static String toDisplayCase (String anyCase, char wordMarker) {

    StringBuilder displayBuilder;
    boolean newWord = true;

    displayBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        newWord = true;
        displayBuilder.append(" ");
      } else if (newWord) {
        displayBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
        newWord = false;
      } else {
        displayBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
      }
    }

    return displayBuilder.toString();
  }

  /**
   * Converts a word-delimited string to upper camel case (PascalCase), capitalizing the first letter of every word and removing the delimiter.
   *
   * @param anyCase    the source string to convert
   * @param wordMarker the character that separates words
   * @return the camel-cased string with its first character uppercased
   */
  public static String toCamelCase (String anyCase, char wordMarker) {

    return toCamelCase(anyCase, wordMarker, true);
  }

  /**
   * Converts a word-delimited string to camel case, optionally starting with an uppercase or lowercase first character.
   *
   * @param anyCase    the source string to convert
   * @param wordMarker the character that separates words
   * @param startUpper {@code true} to uppercase the very first character (PascalCase), {@code false} for lower camelCase
   * @return the camel-cased string
   */
  public static String toCamelCase (String anyCase, char wordMarker, boolean startUpper) {

    StringBuilder camelBuilder;
    boolean upper = startUpper;

    camelBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        upper = true;
      } else if (upper) {
        camelBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
        upper = false;
      } else {
        camelBuilder.append(Character.toLowerCase(anyCase.charAt(count)));
      }
    }

    return camelBuilder.toString();
  }

  /**
   * Converts a word-delimited string to a Java static constant name by uppercasing all characters and replacing the delimiter with underscores.
   *
   * @param anyCase    the source string to convert
   * @param wordMarker the character that separates words, replaced by {@code '_'} in the result
   * @return the uppercase, underscore-delimited constant name
   */
  public static String toStaticFieldName (String anyCase, char wordMarker) {

    StringBuilder fieldBuilder;

    fieldBuilder = new StringBuilder();

    for (int count = 0; count < anyCase.length(); count++) {
      if (anyCase.charAt(count) == wordMarker) {
        fieldBuilder.append('_');
      } else {
        fieldBuilder.append(Character.toUpperCase(anyCase.charAt(count)));
      }
    }

    return fieldBuilder.toString();
  }

  /**
   * Returns the text unchanged if it fits within {@code maxLength}, otherwise truncates it and appends {@code "..."}.
   *
   * @param text      the source text to trim
   * @param maxLength the maximum length of the returned string, including the ellipsis; must be at least 4
   * @return the original text if its length is at most {@code maxLength}, otherwise the text truncated to {@code maxLength - 3} characters followed by {@code "..."}
   * @throws IllegalArgumentException if {@code maxLength} is less than 4
   */
  public static String trimWithElipses (String text, int maxLength) {

    int length = text.length();

    if (length < 4) {
      throw new IllegalArgumentException("maxLength < 4");
    } else if (length <= maxLength) {

      return text;
    } else {

      return text.substring(0, maxLength - 3) + "...";
    }
  }

  /**
   * Returns {@code true} if the supplied string is a syntactically valid Java identifier.
   *
   * @param anyName the string to validate
   * @return {@code true} if every character satisfies the Java identifier rules
   */
  public static boolean isJavaIdentifier (String anyName) {

    for (int count = 0; count < anyName.length(); count++) {
      if (count == 0) {
        if (!Character.isJavaIdentifierStart(anyName.charAt(count))) {
          return false;
        }
      } else if (!Character.isJavaIdentifierPart(anyName.charAt(count))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns {@code true} if {@code match} appears in {@code template} starting exactly at {@code index}.
   *
   * @param template the string to search within
   * @param match    the substring to look for
   * @param index    the zero-based starting position in {@code template}
   * @return {@code true} if {@code match} is fully present starting at {@code index}
   */
  public static boolean hasNext (String template, String match, int index) {

    int matchIndex = 0;

    while ((matchIndex < match.length()) && (index < template.length()) && (template.charAt(index) == match.charAt(matchIndex))) {
      index += 1;
      matchIndex += 1;
    }

    return matchIndex == match.length();
  }
}
