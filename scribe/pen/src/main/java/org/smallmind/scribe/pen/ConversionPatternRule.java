/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

public class ConversionPatternRule implements PatternRule {

  private enum Padding {

    LEFT, RIGHT, NONE
  }

  private String header;
  private String footer;
  private String multiLinePrefix;
  private Padding padding;
  private boolean prefixFirstLine;
  private char conversion;
  private int width;
  private int precision;

  public ConversionPatternRule (String header, String paddingString, String widthString, String precisionString, String firstLineString, String multiLinePrefix, String conversionString, String footer) {

    this(header, (paddingString == null) ? Padding.NONE : (paddingString.equals("+") ? Padding.RIGHT : Padding.LEFT), (widthString == null) ? -1 : Integer.parseInt(widthString), (precisionString == null) ? -1 : Integer.parseInt(precisionString), !("-".equals(firstLineString)), (multiLinePrefix == null) ? System.getProperty("line.separator") + '\t' : multiLinePrefix, conversionString.charAt(0), footer);
  }

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

  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
  }

  public String getHeader () {

    return header;
  }

  public String getFooter () {

    return footer;
  }

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

  public String convert (Record record, Filter[] filters, Timestamp timestamp) {

    LogicalContext logicalContext;
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
        if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {
          return trimToWidthAndPad(trimToDotPrecision(logicalContext.getClassName()));
        }

        return null;
      case 'M':
        if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {
          return trimToWidthAndPad(logicalContext.getMethodName());
        }

        return null;
      case 'N':
        if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {
          return trimToWidthAndPad(String.valueOf(logicalContext.isNativeMethod()));
        }

        return null;
      case 'L':
        if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {
          return trimToWidthAndPad(String.valueOf(logicalContext.getLineNumber()));
        }

        return null;
      case 'F':
        if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {
          return trimToWidthAndPad(logicalContext.getFileName());
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

          paddingBuilder = new StringBuilder(field);

          for (int count = 0; count < width - field.length(); count++) {
            paddingBuilder.append(' ');
          }

          return paddingBuilder.toString();
        case LEFT:

          paddingBuilder = new StringBuilder();

          for (int count = 0; count < width - field.length(); count++) {
            paddingBuilder.append(' ');
          }
          paddingBuilder.append(field);

          return paddingBuilder.toString();
        default:
          throw new UnknownSwitchCaseException(padding.name());
      }
    } else if (width > 0) {

      return field.substring(0, width);
    }

    return field;
  }
}