/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class Glob {

  private enum State {NORMAL, ESCAPED, BRACKETED, RANGED, GROUPED}

  private static final String GLOBAL_META = "\\*?[{";
  private static final String REGEX_META = ".^$+{[]|()";

  public static Pattern toRegexPattern (char separator, String globPattern) {

    StringBuilder regex = new StringBuilder("^");
    State state = State.NORMAL;
    State originalState = null;
    int bracketMarker = 0;
    int rangeMarker = 0;

    for (int index = 0; index < globPattern.length(); index++) {

      char aChar = globPattern.charAt(index);

      switch (state) {
        case ESCAPED:
          if ((GLOBAL_META.indexOf(aChar) > 0) || (REGEX_META.indexOf(aChar) > 0)) {
            regex.append('\\');
          }

          regex.append(aChar);
          state = originalState;
          break;
        case BRACKETED:
          if (separator == aChar) {
            throw new PatternSyntaxException("Explicit path separator in class", globPattern, index);
          } else {
            switch (aChar) {
              case ']':
                if ((index == bracketMarker) || ((index == bracketMarker + 1) && (globPattern.charAt(bracketMarker) == '!'))) {
                  regex.append(']');
                } else {
                  regex.append("]]");
                  state = originalState;
                  break;
                }
              case '^':
                if (index == bracketMarker) {
                  regex.append("\\");
                }
                regex.append(aChar);
                break;
              case '!':
                regex.append((index == bracketMarker) ? '^' : '!');
                break;
              case '-':
                regex.append('-');
                if ((index > bracketMarker + 1) || ((index == bracketMarker + 1) && (globPattern.charAt(bracketMarker) != '!'))) {
                  if (!(index > rangeMarker)) {
                    throw new PatternSyntaxException("Invalid range", globPattern, index);
                  } else {
                    state = State.RANGED;
                  }
                }
                break;
              case '\\':
                regex.append("\\\\");
                break;
              case '[':
                regex.append("\\[");
                break;
              case '&':
                if ((index > bracketMarker) && (globPattern.charAt(index - 1) == '&')) {
                  regex.append('\\');
                }
                regex.append('&');
                break;
              default:
                regex.append(aChar);
            }
          }
          break;
        case RANGED:
          if (separator == aChar) {
            throw new PatternSyntaxException("Explicit path separator in class", globPattern, index);
          } else if (aChar == ']') {
            throw new PatternSyntaxException("Invalid range", globPattern, index);
          } else {
            if ((aChar == '\\') || (aChar == '[')) {
              regex.append('\\');
            }
            regex.append(aChar);
            rangeMarker = index + 1;
            state = State.BRACKETED;
          }
          break;
        case GROUPED:
          switch (aChar) {
            case '\\':
              originalState = State.GROUPED;
              state = State.ESCAPED;
              break;
            case '[':
              regex.append("[[^/]&&[");
              bracketMarker = index + 1;
              rangeMarker = index + 1;
              originalState = State.GROUPED;
              state = State.BRACKETED;
              break;
            case '{':
              throw new PatternSyntaxException("Illegal attempt to nest groups", globPattern, index);
            case '}':
              regex.append("))");
              state = State.NORMAL;
              break;
            case ',':
              regex.append(")|(?:");
              break;
            case '*':
              if ((index < globPattern.length() - 1) && (globPattern.charAt(index + 1) == '*')) {
                // ignore path separators
                regex.append(".*");
                index++;
              } else {
                regex.append("[^/]*");
              }
              break;
            case '?':
              regex.append("[^/]");
              break;
            default:
              if ((REGEX_META.indexOf(aChar) > 0)) {
                regex.append('\\');
              }
              regex.append(aChar);
          }
          break;
        case NORMAL:
          switch (aChar) {
            case '\\':
              originalState = State.NORMAL;
              state = State.ESCAPED;
              break;
            case '[':
              regex.append("[[^/]&&[");
              bracketMarker = index + 1;
              rangeMarker = index + 1;
              originalState = State.NORMAL;
              state = State.BRACKETED;
              break;
            case '{':
              regex.append("(?:(?:");
              state = State.GROUPED;
              break;
            case '*':
              if ((index < globPattern.length() - 1) && (globPattern.charAt(index + 1) == '*')) {
                // ignore path separators
                regex.append(".*");
                index++;
              } else {
                regex.append("[^/]*");
              }
              break;
            case '?':
              regex.append("[^/]");
              break;
            default:
              if ((REGEX_META.indexOf(aChar) > 0)) {
                regex.append('\\');
              }
              regex.append(aChar);
          }
          break;
      }
    }

    switch (state) {
      case ESCAPED:
        throw new PatternSyntaxException("No character to escape", globPattern, globPattern.length() - 1);
      case BRACKETED:
        throw new PatternSyntaxException("Missing class terminator ']'", globPattern, globPattern.length() - 1);
      case RANGED:
        throw new PatternSyntaxException("Invalid range", globPattern, globPattern.length() - 1);
      case GROUPED:
        throw new PatternSyntaxException("Missing group terminator '}'", globPattern, globPattern.length() - 1);
    }

    return Pattern.compile(regex.append('$').toString());
  }
}
