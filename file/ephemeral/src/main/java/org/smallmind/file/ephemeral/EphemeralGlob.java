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
package org.smallmind.file.ephemeral;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class that translates glob expressions into equivalent compiled regular expressions.
 * This class is not intended to be instantiated.
 *
 * <p>Supported glob constructs:
 * <ul>
 *   <li>{@code *} — matches any sequence of characters that does not contain the path
 *       separator</li>
 *   <li>{@code **} — matches any sequence of characters including path separators</li>
 *   <li>{@code ?} — matches exactly one character that is not the path separator</li>
 *   <li>{@code [abc]}, {@code [a-z]}, {@code [!abc]} — character classes</li>
 *   <li>{@code {a,b,c}} — alternation groups</li>
 *   <li>{@code \} — escape character</li>
 * </ul>
 */
public class EphemeralGlob {

  private static final String GLOB_META_CHARACTERS = "\\*?[{";
  private static final String REGEX_META_CHARACTERS = ".^$+{[]|()";

  /**
   * Converts a glob expression into a compiled {@link Pattern} anchored at both ends
   * ({@code ^…$}). The supplied separator character is used to prevent wildcard tokens from
   * matching path boundaries where the glob specification requires it.
   *
   * @param separator   the path separator character (e.g., {@code '/'}) that single-segment
   *                    wildcards ({@code *} and {@code ?}) must not match
   * @param globPattern the glob expression to translate; must not be {@code null}
   * @return a compiled {@link Pattern} equivalent to the supplied glob expression
   * @throws PatternSyntaxException if the glob expression is syntactically invalid (e.g.,
   *                                an unterminated character class, group, or escape sequence,
   *                                or an explicit separator within a character class)
   */
  public static Pattern toRegexPattern (char separator, String globPattern) {

    StringBuilder regexBuilder = new StringBuilder("^");
    String notSeparator = notSeparatorClass(separator);
    boolean inGroup = false;
    int index = 0;

    while (index < globPattern.length()) {

      char globChar = globPattern.charAt(index++);

      switch (globChar) {
        case '\\':
          if (index == globPattern.length()) {
            throw new PatternSyntaxException("No character to escape", globPattern, index - 1);
          } else {

            char escapedChar = globPattern.charAt(index++);

            if ((GLOB_META_CHARACTERS.indexOf(escapedChar) >= 0) || (REGEX_META_CHARACTERS.indexOf(escapedChar) >= 0)) {
              regexBuilder.append('\\');
            }
            regexBuilder.append(escapedChar);
          }
          break;
        case '[':
          index = appendCharacterClass(regexBuilder, globPattern, index, separator, notSeparator);
          break;
        case '{':
          if (inGroup) {
            throw new PatternSyntaxException("Illegal attempt to nest groups", globPattern, index - 1);
          } else {
            regexBuilder.append("(?:(?:");
            inGroup = true;
          }
          break;
        case '}':
          if (inGroup) {
            regexBuilder.append("))");
            inGroup = false;
          } else {
            regexBuilder.append("\\}");
          }
          break;
        case ',':
          regexBuilder.append(inGroup ? ")|(?:" : ",");
          break;
        case '*':
          if ((index < globPattern.length()) && (globPattern.charAt(index) == '*')) {
            // ignore path separators
            regexBuilder.append(".*");
            index++;
          } else {
            regexBuilder.append(notSeparator).append('*');
          }
          break;
        case '?':
          regexBuilder.append(notSeparator);
          break;
        default:
          if (REGEX_META_CHARACTERS.indexOf(globChar) >= 0) {
            regexBuilder.append('\\');
          }
          regexBuilder.append(globChar);
      }
    }

    if (inGroup) {
      throw new PatternSyntaxException("Missing group terminator '}'", globPattern, index - 1);
    }

    return Pattern.compile(regexBuilder.append('$').toString());
  }

  /**
   * Translates a bracketed glob character class, beginning immediately after the opening
   * {@code '['}, into a regular expression class intersected with "any character but the path
   * separator".
   *
   * @param regexBuilder the builder accumulating the translated expression
   * @param globPattern  the glob expression being translated
   * @param index        the index of the first character inside the class
   * @param separator    the path separator character, which may not appear within a class
   * @param notSeparator the regular expression class matching any character but the separator
   * @return the index of the first character following the class terminator
   * @throws PatternSyntaxException if the class is unterminated, contains an explicit path
   *                                separator, or declares an invalid range
   */
  private static int appendCharacterClass (StringBuilder regexBuilder, String globPattern, int index, char separator, String notSeparator) {

    boolean hasRangeStart = false;
    boolean terminated = false;
    char lastChar = 0;

    regexBuilder.append('[').append(notSeparator).append("&&[");

    if (charAt(globPattern, index) == '^') {
      regexBuilder.append("\\^");
      index++;
    } else {
      if (charAt(globPattern, index) == '!') {
        regexBuilder.append('^');
        index++;
      }
      if (charAt(globPattern, index) == '-') {
        regexBuilder.append('-');
        index++;
      }
    }

    while (index < globPattern.length()) {

      char classChar = globPattern.charAt(index++);

      if (classChar == ']') {
        terminated = true;
        break;
      } else if (classChar == separator) {
        throw new PatternSyntaxException("Explicit path separator in class", globPattern, index - 1);
      } else if (classChar == '-') {
        if (!hasRangeStart) {
          throw new PatternSyntaxException("Invalid range", globPattern, index - 1);
        } else {

          char rangeEndChar = charAt(globPattern, index);

          if ((rangeEndChar == 0) || (rangeEndChar == ']')) {
            regexBuilder.append('-');
          } else if (rangeEndChar < lastChar) {
            throw new PatternSyntaxException("Invalid range", globPattern, index);
          } else {
            regexBuilder.append('-').append(rangeEndChar);
            index++;
          }

          hasRangeStart = false;
        }
      } else {
        if ((classChar == '\\') || (classChar == '[') || ((classChar == '&') && (charAt(globPattern, index) == '&'))) {
          regexBuilder.append('\\');
        }
        regexBuilder.append(classChar);
        hasRangeStart = true;
        lastChar = classChar;
      }
    }

    if (!terminated) {
      throw new PatternSyntaxException("Missing class terminator ']'", globPattern, index - 1);
    }

    regexBuilder.append("]]");

    return index;
  }

  /**
   * Renders the regular expression character class matching any single character other than the
   * supplied path separator.
   *
   * @param separator the path separator character to exclude
   * @return a regular expression character class, such as {@code "[^/]"}
   */
  private static String notSeparatorClass (char separator) {

    return Character.isLetterOrDigit(separator) ? ("[^" + separator + ']') : ("[^\\" + separator + ']');
  }

  /**
   * Returns the character at the given index, or {@code 0} when the index lies past the end of
   * the expression.
   *
   * @param globPattern the glob expression being translated
   * @param index       the index to read
   * @return the character at {@code index}, or {@code 0} past the end of the expression
   */
  private static char charAt (String globPattern, int index) {

    return (index < globPattern.length()) ? globPattern.charAt(index) : 0;
  }
}
