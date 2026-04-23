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

import java.util.regex.Pattern;

/**
 * Compiles glob-style dot-notation patterns (using {@code .} separators and {@code *}/{@code ?} wildcards)
 * into regular expressions and computes a bitmask value reflecting the number and kind of segments matched.
 */
public class DotNotation {

  private enum TranslationState {

    START, POST_DOT, WILD, NORMAL
  }

  private enum WildState {

    STAR, QUESTION, TAME
  }

  private Pattern pattern;
  private int value;

  /**
   * Constructs an uninitialized instance; {@link #setNotation(String)} must be called before use.
   */
  public DotNotation () {

  }

  /**
   * Constructs a {@code DotNotation} by immediately parsing the given pattern.
   *
   * @param notation the dot-notation pattern to compile
   * @throws DotNotationException if the pattern is malformed
   */
  public DotNotation (String notation)
    throws DotNotationException {

    setNotation(notation);
  }

  /**
   * Returns the compiled {@link Pattern} produced by parsing the most recent notation, or {@code null} if none was set.
   *
   * @return the compiled regex pattern
   */
  public Pattern getPattern () {

    return pattern;
  }

  /**
   * Returns the bitmask value accumulated during parsing, where each distinct segment contributes a power-of-two bit.
   *
   * @return the segment bitmask value
   */
  public int getValue () {

    return value;
  }

  /**
   * Parses the given dot-notation pattern, compiles it into a {@link Pattern}, and stores the resulting bitmask value.
   *
   * @param notation the pattern to parse, consisting of Java identifier segments separated by dots and optional wildcards
   * @return this instance for method chaining
   * @throws DotNotationException if the pattern is malformed
   */
  public DotNotation setNotation (String notation)
    throws DotNotationException {

    RegexConversion conversion;

    conversion = createRegex(notation);
    pattern = Pattern.compile(conversion.regex());
    value = conversion.value();

    return this;
  }

  /**
   * Returns this notation's bitmask value when the given name matches the compiled pattern, or {@code initial} otherwise.
   *
   * @param name    the name to test against the compiled pattern
   * @param initial the value to return when the pattern is unset or the name does not match
   * @return the notation's bitmask value on a match, or {@code initial} on no match
   */
  public int calculateValue (String name, int initial) {

    return (pattern == null) ? initial : pattern.matcher(name).matches() ? value : initial;
  }

  /**
   * Translates a dot-notation pattern string into a {@link RegexConversion} holding the compiled regex and bitmask value.
   *
   * @param notation the pattern to translate, using Java identifier segments, dots, and {@code *}/{@code ?} wildcards
   * @return a {@link RegexConversion} with the regex string and the computed bitmask value
   * @throws DotNotationException if the pattern violates structural rules such as consecutive dots or misplaced wildcards
   */
  private RegexConversion createRegex (String notation)
    throws DotNotationException {

    StringBuilder patternBuilder;
    TranslationState translationState;
    WildState wildState;
    char curChar;
    int index;
    int segment = 0;
    int value = 0;

    patternBuilder = new StringBuilder();
    patternBuilder.append('^');
    translationState = TranslationState.START;
    wildState = WildState.TAME;

    for (index = 0; index < notation.length(); index++) {
      curChar = notation.charAt(index);
      switch (curChar) {
        case '.':
          if (translationState.equals(TranslationState.POST_DOT)) {
            throw new DotNotationException("Empty component in the pattern");
          } else if (translationState.equals(TranslationState.START)) {
            throw new DotNotationException("The pattern can not begin with '.'");
          }

          if (translationState.equals(TranslationState.NORMAL)) {
            patternBuilder.append(')');
            value += (int)Math.pow(2, ++segment);
          }

          patternBuilder.append("(\\.|\\$)");
          translationState = TranslationState.POST_DOT;

          break;
        case '*':
          if (translationState.equals(TranslationState.WILD)) {
            throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
          } else if (translationState.equals(TranslationState.NORMAL)) {
            throw new DotNotationException("Wildcards must either start the pattern or be preceded by '.'");
          } else if (!wildState.equals(WildState.TAME)) {
            throw new DotNotationException("Any wild card followed by '*' is redundant");
          }

          patternBuilder.append("(.+)");
          translationState = TranslationState.WILD;
          wildState = WildState.STAR;
          value += (int)(Math.pow(2, ++segment) - 1);

          break;
        case '?':
          if (translationState.equals(TranslationState.WILD)) {
            throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
          } else if (translationState.equals(TranslationState.NORMAL)) {
            throw new DotNotationException("Wildcards must either start the pattern or be preceded by '.'");
          } else if (wildState.equals(WildState.STAR)) {
            throw new DotNotationException("Following '*' with '?' is redundant");
          }

          patternBuilder.append("([^.$]+)");
          translationState = TranslationState.WILD;
          wildState = WildState.QUESTION;
          value += (int)(Math.pow(2, ++segment) - 1);

          break;
        default:
          if (!Character.isJavaIdentifierPart(curChar)) {
            throw new DotNotationException("Components must be composed of valid Java identifiers");
          } else if (translationState.equals(TranslationState.WILD)) {
            throw new DotNotationException("Wildcards must either be followed by '.' or terminate the pattern");
          }

          if ((translationState.equals(TranslationState.POST_DOT)) || (translationState.equals(TranslationState.START))) {
            patternBuilder.append('(');
          }

          patternBuilder.append(curChar);
          translationState = TranslationState.NORMAL;
          wildState = WildState.TAME;
      }
    }

    if (translationState.equals(TranslationState.POST_DOT)) {
      throw new DotNotationException("The pattern can not end with '.'");
    } else if (translationState.equals(TranslationState.NORMAL)) {
      patternBuilder.append(')');
      value += (int)Math.pow(2, ++segment);
    }

    patternBuilder.append('$');

    return new RegexConversion(patternBuilder.toString(), value);
  }

  /**
   * Holds the results of converting a dot-notation pattern: the compiled regex string and the segment bitmask value.
   *
   * @param regex the regex string derived from the dot-notation pattern
   * @param value the bitmask value accumulated from the pattern's segments
   */
  private record RegexConversion(String regex, int value) {

    private RegexConversion {

    }

    /**
     * Returns the bitmask value accumulated from the parsed dot-notation segments.
     *
     * @return the segment bitmask value
     */
    @Override
    public int value () {

      return value;
    }
  }
}
