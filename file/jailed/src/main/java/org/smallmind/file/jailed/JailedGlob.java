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
package org.smallmind.file.jailed;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class that translates a glob expression into an equivalent regular expression over
 * jailed path strings. This class is not intended to be instantiated.
 *
 * <p>The translation is performed in terms of the jailed separator, the forward slash, and is
 * deliberately independent of the backing native file system: the same jailed glob selects the
 * same jailed paths whether the jail is backed by Windows, by Linux, or by another provider
 * entirely. Matching is case-sensitive, as jail space itself is.
 *
 * <p>Supported glob constructs, following the syntax documented by
 * {@link java.nio.file.FileSystem#getPathMatcher(String)}:
 * <ul>
 *   <li>{@code *} - matches any sequence of characters within a single name element</li>
 *   <li>{@code **} - matches any sequence of characters, crossing name elements</li>
 *   <li>{@code ?} - matches exactly one character within a single name element</li>
 *   <li>{@code [abc]}, {@code [a-z]}, {@code [!abc]} - character classes, which never match
 *       the separator</li>
 *   <li>{@code {a,b,c}} - alternation groups, which may not be nested</li>
 *   <li>{@code \} - escapes the character that follows it</li>
 * </ul>
 *
 * @see JailedPathMatcher
 */
public class JailedGlob {

  /**
   * The characters that carry meaning within a glob expression, and so must be escaped in order
   * to be matched literally.
   */
  private static final String GLOB_META_CHARACTERS = "\\*?[{";

  /**
   * The characters that carry meaning within a regular expression, and so must be escaped when
   * emitted as literal text.
   */
  private static final String REGEX_META_CHARACTERS = ".^$+{[]|()";

  /**
   * Translates a glob expression into a compiled {@link Pattern} anchored at both ends.
   *
   * @param globPattern the glob expression to translate; must not be {@code null}
   * @return a compiled {@link Pattern} equivalent to the supplied glob expression
   * @throws PatternSyntaxException if the glob expression is syntactically invalid, for example
   *                                an unterminated character class, group or escape sequence, an
   *                                inverted range, a nested group, or an explicit separator
   *                                within a character class
   */
  public static Pattern toRegexPattern (String globPattern) {

    StringBuilder regexBuilder = new StringBuilder("^");
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
          index = appendCharacterClass(regexBuilder, globPattern, index);
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
            regexBuilder.append(".*");
            index++;
          } else {
            regexBuilder.append("[^/]*");
          }
          break;
        case '?':
          regexBuilder.append("[^/]");
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
   * Translates the body of a glob character class, which has already had its opening bracket
   * consumed, appending the equivalent regular expression construct.
   *
   * <p>The emitted class is intersected with the set of characters that are not the separator,
   * so that a class can never match across a name element boundary.
   *
   * @param regexBuilder the builder accumulating the translated expression
   * @param globPattern  the glob expression being translated
   * @param index        the index of the first character within the class
   * @return the index of the first character following the class
   * @throws PatternSyntaxException if the class is unterminated, contains an explicit separator,
   *                                or contains an invalid range
   */
  private static int appendCharacterClass (StringBuilder regexBuilder, String globPattern, int index) {

    boolean hasRangeStart = false;
    boolean terminated = false;
    char lastChar = 0;

    regexBuilder.append("[[^/]&&[");

    if (charAt(globPattern, index) == '^') {
      // a leading '^' is literal in a glob, but would negate a regular expression class
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
      } else if (classChar == JailedPath.SEPARATOR) {
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
   * Returns the character at a position within the glob expression, or {@code 0} if the position
   * is past its end.
   *
   * @param globPattern the glob expression being translated
   * @param index       the position to read
   * @return the character at {@code index}, or {@code 0} at the end of the expression
   */
  private static char charAt (String globPattern, int index) {

    return (index < globPattern.length()) ? globPattern.charAt(index) : 0;
  }
}
