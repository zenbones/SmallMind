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

import java.util.Date;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Implements a single conversion token for {@link PatternFormatter}, handling padding, precision, and multi-line options.
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
   * Creates a rule from the parsed pattern components.
   *
   * @param header           optional header text
   * @param paddingString    padding flag (+/-) or {@code null}
   * @param widthString      width specifier or {@code null}
   * @param precisionString  precision specifier or {@code null}
   * @param firstLineString  flag for multi-line prefixing of first line
   * @param multiLinePrefix  prefix to apply to multi-line fields
   * @param conversionString conversion character as string
   * @param footer           optional footer text
   */
  public ConversionPatternRule (String header, String paddingString, String widthString, String precisionString, String firstLineString, String multiLinePrefix, String conversionString, String footer) {

    this(header, (paddingString == null) ? Padding.NONE : (paddingString.equals("+") ? Padding.RIGHT : Padding.LEFT), (widthString == null) ? -1 : Integer.parseInt(widthString), (precisionString == null) ? -1 : Integer.parseInt(precisionString), !("-".equals(firstLineString)), (multiLinePrefix == null) ? System.getProperty("line.separator") + '\t' : multiLinePrefix, conversionString.charAt(0), footer);
  }

  /**
   * Creates a rule with explicit padding, width, precision, and conversion token.
   *
   * @param header          optional header text
   * @param padding         padding direction
   * @param width           fixed width or -1 if unrestricted
   * @param precision       precision for dot-separated fields or -1 if unrestricted
   * @param prefixFirstLine whether to prefix the first line for multi-line fields
   * @param multiLinePrefix prefix to apply to multi-line output
   * @param conversion      conversion character to render
   * @param footer          optional footer text
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
   * Finds the number of repeated elements between the current and previous stack traces.
   *
   * @param singleElement  element from the current stack trace
   * @param prevStackTrace previous stack trace to compare
   * @return count of remaining repeated elements, or -1 if none
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
   * Returns the header text for this rule.
   *
   * @return header text, possibly {@code null}
   */
  public String getHeader () {

    return header;
  }

  /**
   * Returns the footer text for this rule.
   *
   * @return footer text, possibly {@code null}
   */
  public String getFooter () {

    return footer;
  }

  /**
   * Unescapes common slash-escaped sequences within the provided string.
   *
   * @param slashedString string containing escape sequences
   * @return unescaped string, or {@code null} if input was {@code null}
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
   * Renders the requested conversion value from the record using the provided timestamp.
   *
   * @param record    record to format
   * @param timestamp timestamp renderer used for date conversions
   * @return formatted value or {@code null} when no value is available
   */
  public String convert (Record<?> record, Timestamp timestamp) {

    LoggerContext loggerContext;
    Throwable throwable = record.getThrown();
    Parameter[] parameters;

    switch (conversion) {
      case 'd':

        return trimToWidthAndPad(timestamp.getTimestamp(new Date(record.getMillis())));
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
   * Trims a dot-delimited field to the configured precision.
   *
   * @param field dot-delimited value
   * @return field truncated to the last {@code precision} segments, or original value when precision is unset
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
   * Applies width and padding rules to the provided field.
   *
   * @param field value to pad or trim
   * @return adjusted value respecting width and padding settings
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

          return new StringBuilder(field).append(" ".repeat(width - field.length())).toString();
        case LEFT:

          return new StringBuilder(" ".repeat(width - field.length())).append(field).toString();
        default:
          throw new UnknownSwitchCaseException(padding.name());
      }
    } else if (width > 0) {

      return field.substring(0, width);
    }

    return field;
  }
}
