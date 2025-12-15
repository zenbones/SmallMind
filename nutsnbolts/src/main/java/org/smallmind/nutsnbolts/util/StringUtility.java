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
 * Case and identifier helpers for strings.
 */
public class StringUtility {

  /**
   * Converts a string to display case, capitalizing the first character of each word and lowercasing the rest.
   *
   * @param anyCase source string
   * @return display-cased string
   */
  public static String toDisplayCase (String anyCase) {

    return toDisplayCase(anyCase, '\0');
  }

  /**
   * Converts a string to display case using a custom word separator character that will be replaced by spaces.
   *
   * @param anyCase    source string
   * @param wordMarker character that separates words
   * @return display-cased string with separators replaced by spaces
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
   * Converts a word-delimited string to camel case, capitalizing each word after the first.
   *
   * @param anyCase    source string
   * @param wordMarker delimiter separating words
   * @return camel-cased string starting with an uppercase letter
   */
  public static String toCamelCase (String anyCase, char wordMarker) {

    return toCamelCase(anyCase, wordMarker, true);
  }

  /**
   * Converts a word-delimited string to camel case.
   *
   * @param anyCase    source string
   * @param wordMarker delimiter separating words
   * @param startUpper whether the first character should be uppercase
   * @return camel-cased string
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
   * Converts a word-delimited string to an uppercase-with-underscores constant name.
   *
   * @param anyCase    source string
   * @param wordMarker delimiter separating words
   * @return uppercase string with underscores replacing delimiters
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
   * Trims a string to a maximum length, appending "..." when truncated.
   *
   * @param text      source text
   * @param maxLength maximum allowed length (must be at least 4)
   * @return original text if short enough; otherwise truncated with ellipses
   * @throws IllegalArgumentException if {@code maxLength < 4}
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
   * Validates whether a string is a legal Java identifier.
   *
   * @param anyName string to check
   * @return {@code true} if the name is a valid identifier
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
   * Tests whether a given substring exists in the template starting at the provided index.
   *
   * @param template full string
   * @param match    substring to test
   * @param index    starting offset in the template
   * @return {@code true} if the substring fully matches starting at the index
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
