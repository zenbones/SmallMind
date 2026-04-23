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
package org.smallmind.scribe.pen;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Pattern rule that renders one conversion token from a {@link PatternFormatter} pattern, applying
 * optional fixed-width padding (left or right), dot-notation precision truncation, and multi-line
 * prefix handling for stack-trace ({@code %s}) and parameter ({@code %p}) fields.
 */
public class ConversionPatternRule implements PatternRule {

  private enum Padding {

    LEFT, RIGHT, NONE
  }

  private final String header;
  private final String footer;
  private final String multiLinePrefix;
  private final Padding padding;
  private final boolean prefixFirstLine;
  private final char conversion;
  private final int width;
  private final int precision;

  /**
   * Constructs a rule by parsing the raw string components captured by the {@link PatternFormatter}
   * regex. Each argument corresponds to a named capture group; {@code null} values are replaced with
   * appropriate defaults before delegating to the fully-typed constructor.
   *
   * @param header           optional header text emitted before the field value, or {@code null}
   * @param paddingString    {@code "+"} for right-padding, {@code "-"} for left-padding, or {@code null} for no padding
   * @param widthString      decimal string specifying the fixed field width, or {@code null} for unrestricted
   * @param precisionString  decimal string specifying the precision (dot segments or max parameter lines), or {@code null}
   * @param firstLineString  {@code "-"} to suppress the multi-line prefix on the first line; any other value or {@code null} to include it
   * @param multiLinePrefix  text inserted before each line of a multi-line field, or {@code null} for the default newline-tab
   * @param conversionString single-character string identifying the conversion (e.g. {@code "d"}, {@code "m"})
   * @param footer           optional footer text emitted after the field value, or {@code null}
   */
  public ConversionPatternRule (String header, String paddingString, String widthString, String precisionString, String firstLineString, String multiLinePrefix, String conversionString, String footer) {

    this(header, (paddingString == null) ? Padding.NONE : (paddingString.equals("+") ? Padding.RIGHT : Padding.LEFT), (widthString == null) ? -1 : Integer.parseInt(widthString), (precisionString == null) ? -1 : Integer.parseInt(precisionString), !("-".equals(firstLineString)), (multiLinePrefix == null) ? System.getProperty("line.separator") + '\t' : multiLinePrefix, conversionString.charAt(0), footer);
  }

  /**
   * Constructs a fully-typed rule with explicit formatting options. Header, multi-line prefix, and
   * footer strings are passed through {@link #stripSlashes(String)} to resolve escape sequences.
   *
   * @param header          optional header text emitted before the field value, or {@code null}
   * @param padding         direction in which to pad the value when it is shorter than {@code width}
   * @param width           fixed field width for padding and truncation, or {@code -1} for unrestricted
   * @param precision       maximum dot-notation segments (for {@code n}, {@code C}) or max parameter lines
   *                        (for {@code p}), or {@code -1} for unrestricted
   * @param prefixFirstLine {@code true} to apply {@code multiLinePrefix} before the first output line
   *                        of a multi-line field
   * @param multiLinePrefix text prepended to each line of a multi-line field
   * @param conversion      the conversion character that identifies which record field to render
   * @param footer          optional footer text emitted after the field value, or {@code null}
   */
  public ConversionPatternRule (String header, Padding padding, int width, int precision, boolean prefixFirstLine, String multiLinePrefix, char conversion, String footer) {

    this.header = stripSlashes(header);
    this.padding = padding;
    this.width = width;
    this.precision = precision;
    this.prefixFirstLine = prefixFirstLine;
    this.multiLinePrefix = stripSlashes(multiLinePrefix);
    this.conversion = conversion;
    this.footer = stripSlashes(footer);
  }

  /**
   * Searches the previous stack trace for the given element and returns how many frames remain
   * at and after the match, enabling the "... N more" abbreviation in chained exception output.
   *
   * @param singleElement  the frame from the current throwable's stack trace to search for
   * @param prevStackTrace the stack trace of the enclosing (previously rendered) throwable
   * @return the number of frames in {@code prevStackTrace} at and after the matching position,
   * or {@code -1} if no matching frame is found
   */
  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
  }

  /**
   * Returns the header text that is prepended to the converted field value when the value is non-{@code null}.
   *
   * @return the header text, or {@code null} if no header was configured
   */
  public String getHeader () {

    return header;
  }

  /**
   * Returns the footer text that is appended to the converted field value when the value is non-{@code null}.
   *
   * @return the footer text, or {@code null} if no footer was configured
   */
  public String getFooter () {

    return footer;
  }

  /**
   * Resolves backslash escape sequences in the given string, converting {@code \r}, {@code \t},
   * {@code \f}, and {@code \n} to their corresponding control characters (where {@code \n} becomes
   * the platform line separator). Unrecognized escape sequences have their backslash removed and the
   * following character is kept as-is. A trailing lone backslash is preserved literally.
   *
   * @param slashedString the raw string that may contain backslash escape sequences, or {@code null}
   * @return the unescaped string, or {@code null} if the input was {@code null}
   */
  private String stripSlashes (String slashedString) {

    if (slashedString == null) {

      return null;
    }

    StringBuffer strippedBuffer;
    boolean slashed = false;

    strippedBuffer = new StringBuffer();

    for (int count = 0; count < slashedString.length(); count++) {
      if (slashed) {
        switch (slashedString.charAt(count)) {
          case 'r':
            strippedBuffer.append('\r');
            break;
          case 't':
            strippedBuffer.append('\t');
            break;
          case 'f':
            strippedBuffer.append('\f');
            break;
          case 'n':
            strippedBuffer.append(System.getProperty("line.separator"));
            break;
          default:
            strippedBuffer.append(slashedString.charAt(count));
        }

        slashed = false;
      } else if (slashedString.charAt(count) == '\\') {
        slashed = true;
      } else {
        strippedBuffer.append(slashedString.charAt(count));
      }
    }

    if (slashed) {
      strippedBuffer.append('\\');
    }

    return strippedBuffer.toString();
  }

  /**
   * Renders the field identified by this rule's conversion character, applying width, padding,
   * precision, and multi-line prefix rules as configured. Returns {@code null} when the requested
   * field is absent from the record (e.g. no logger context, no thrown exception, no parameters),
   * signalling to {@link PatternFormatter} that the header and footer should also be suppressed.
   *
   * @param record    the log record from which field values are extracted
   * @param timestamp the timestamp provider used to render the {@code d} (date) conversion
   * @return the formatted field value, or {@code null} if the field is unavailable for this record
   */
  public String convert (Record<?> record, Timestamp timestamp) {

    LoggerContext loggerContext;
    Throwable throwable = record.getThrown();
    Parameter[] parameters;

    switch (conversion) {
      case 'd':

        return trimToWidthAndPad(timestamp.getTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault())));
      case 't':

        return trimToWidthAndPad(String.valueOf(record.getMillis()));
      case 'n':

        return trimToWidthAndPad(trimToDotPrecision(record.getLoggerName()));
      case 'l':

        return trimToWidthAndPad(record.getLevel().name());
      case 'm':

        String message;

        if ((message = record.getMessage()) == null) {
          if (throwable != null) {
            message = throwable.getMessage();
          }
        }

        return trimToWidthAndPad(message);
      case 'T':

        return trimToWidthAndPad(record.getThreadName());
      case 'C':
        if (((loggerContext = record.getLoggerContext()) != null) && loggerContext.isFilled()) {
          return trimToWidthAndPad(trimToDotPrecision(loggerContext.getClassName()));
        }

        return null;
      case 'M':
        if (((loggerContext = record.getLoggerContext()) != null) && loggerContext.isFilled()) {
          return trimToWidthAndPad(loggerContext.getMethodName());
        }

        return null;
      case 'N':
        if (((loggerContext = record.getLoggerContext()) != null) && loggerContext.isFilled()) {
          return trimToWidthAndPad(String.valueOf(loggerContext.isNativeMethod()));
        }

        return null;
      case 'L':
        if (((loggerContext = record.getLoggerContext()) != null) && loggerContext.isFilled()) {
          return trimToWidthAndPad(String.valueOf(loggerContext.getLineNumber()));
        }

        return null;
      case 'F':
        if (((loggerContext = record.getLoggerContext()) != null) && loggerContext.isFilled()) {
          return trimToWidthAndPad(loggerContext.getFileName());
        }

        return null;
      case 's':
        if (throwable != null) {

          StringBuilder stackBuilder = new StringBuilder();
          StackTraceElement[] prevStackTrace = null;
          int repeatedElements;

          if (prefixFirstLine && (multiLinePrefix != null)) {
            stackBuilder.append(multiLinePrefix);
          }

          do {
            if (prevStackTrace == null) {
              stackBuilder.append("Exception in thread ");
            } else {
              if (prefixFirstLine && (multiLinePrefix != null)) {
                stackBuilder.append(multiLinePrefix);
              }
              stackBuilder.append("Caused by: ");
            }

            stackBuilder.append(throwable.getClass().getCanonicalName());
            stackBuilder.append(": ");
            stackBuilder.append(throwable.getMessage());

            for (StackTraceElement singleElement : throwable.getStackTrace()) {
              if (multiLinePrefix != null) {
                stackBuilder.append(multiLinePrefix);
              }

              if (prevStackTrace != null) {
                if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
                  stackBuilder.append("   ... ");
                  stackBuilder.append(repeatedElements);
                  stackBuilder.append(" more");
                  break;
                }
              }

              stackBuilder.append("   at ");
              stackBuilder.append(singleElement.toString());
            }

            prevStackTrace = throwable.getStackTrace();
          } while ((throwable = throwable.getCause()) != null);

          return stackBuilder.toString();
        }

        return null;
      case 'p':
        if ((parameters = record.getParameters()).length > 0) {

          StringBuilder parameterBuilder = new StringBuilder();
          int parameterCount = 0;

          for (Parameter parameter : parameters) {

            if ((precision > 0) && (++parameterCount > precision)) {
              break;
            }

            if ((prefixFirstLine || (parameterBuilder.length() > 0)) && (multiLinePrefix != null)) {
              parameterBuilder.append(multiLinePrefix);
            }

            parameterBuilder.append(parameter.getKey());
            parameterBuilder.append('=');
            parameterBuilder.append(parameter.getValue());
          }

          if (parameterBuilder.length() > 0) {
            return parameterBuilder.toString();
          }
        }

        return null;
      default:
        throw new UnknownSwitchCaseException("%c", conversion);
    }
  }

  /**
   * Truncates a dot-delimited value to at most the last {@code precision} segments. For example, with
   * {@code precision=2} and the input {@code "com.example.myapp.MyClass"}, the result is
   * {@code "myapp.MyClass"}. If the value has fewer than {@code precision} segments, or if precision
   * is not configured ({@code <= 0}), the original value is returned unchanged.
   *
   * @param field the dot-delimited string to truncate, or {@code null}
   * @return the truncated string, or the original value when precision is unrestricted or there are
   * not enough segments to truncate; {@code null} if the input was {@code null}
   */
  private String trimToDotPrecision (String field) {

    if (field == null) {
      return null;
    }

    if (precision > 0) {

      String[] segments;

      if ((segments = field.split("\\.", -1)).length > precision) {

        StringBuilder segmentBuilder = new StringBuilder();

        for (int count = segments.length - precision; count < segments.length; count++) {
          if (count > (segments.length - precision)) {
            segmentBuilder.append('.');
          }
          segmentBuilder.append(segments[count]);
        }

        return segmentBuilder.toString();
      }
    }

    return field;
  }

  /**
   * Applies the configured width and padding rules to the given field value. If the value is shorter
   * than the width, it is padded on the right (RIGHT padding) or left (LEFT padding); if padding is
   * NONE, short values are returned as-is. If the value is longer than a positive width, it is
   * truncated to exactly {@code width} characters.
   *
   * @param field the string to adjust, or {@code null}
   * @return the padded or truncated string, or {@code null} if the input was {@code null}
   */
  private String trimToWidthAndPad (String field) {

    if (field == null) {
      return null;
    }

    if (field.length() < width) {

      StringBuilder paddingBuilder;

      switch (padding) {
        case NONE:

          return field;
        case RIGHT:

          return field + " ".repeat(width - field.length());
        case LEFT:

          return " ".repeat(width - field.length()) + field;
        default:
          throw new UnknownSwitchCaseException(padding.name());
      }
    } else if (width > 0) {

      return field.substring(0, width);
    }

    return field;
  }
}
