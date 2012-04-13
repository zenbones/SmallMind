/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import java.util.Collection;
import java.util.Date;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.adapter.LoggingBlueprintsFactory;
import org.smallmind.scribe.pen.probe.CompleteOrAbortProbeEntry;
import org.smallmind.scribe.pen.probe.MetricMilieu;
import org.smallmind.scribe.pen.probe.ProbeReport;
import org.smallmind.scribe.pen.probe.Statement;
import org.smallmind.scribe.pen.probe.UpdateProbeEntry;

public class ConversionPatternRule implements PatternRule {

  private static enum Padding {

    LEFT, RIGHT, NONE
  }

  private String header;
  private String footer;
  private String multilinePrefix;
  private Padding padding;
  private boolean prefixFirstLine;
  private char conversion;
  private int width;
  private int precision;

  public ConversionPatternRule (String header, String paddingString, String widthString, String precisionString, String firstLineString, String multilinePrefix, String conversionString, String footer) {

    this(header, (paddingString == null) ? Padding.NONE : (paddingString.equals("+") ? Padding.RIGHT : Padding.LEFT), (widthString == null) ? -1 : Integer.parseInt(widthString), (precisionString == null) ? -1 : Integer.parseInt(precisionString), !("-".equals(firstLineString)), (multilinePrefix == null) ? System.getProperty("line.separator") + '\t' : multilinePrefix, conversionString.charAt(0), footer);
  }

  public ConversionPatternRule (String header, Padding padding, int width, int precision, boolean prefixFirstLine, String multilinePrefix, char conversion, String footer) {

    this.header = stripSlashes(header);
    this.padding = padding;
    this.width = width;
    this.precision = precision;
    this.prefixFirstLine = prefixFirstLine;
    this.multilinePrefix = stripSlashes(multilinePrefix);
    this.conversion = conversion;
    this.footer = stripSlashes(footer);
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
      }
      else if (slashedString.charAt(count) == '\\') {
        slashed = true;
      }
      else {
        strippedBuffer.append(slashedString.charAt(count));
      }
    }

    if (slashed) {
      strippedBuffer.append('\\');
    }

    return strippedBuffer.toString();
  }

  public String convert (Record record, Collection<Filter> filterCollection, Timestamp timestamp) {

    Record filterRecord;
    LogicalContext logicalContext;
    Throwable throwable;
    Parameter[] parameters;
    ProbeReport probeReport;

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

        return trimToWidthAndPad(record.getMessage());
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
      case 'w':
        if ((throwable = record.getThrown()) != null) {

          StringBuilder stackBuilder = new StringBuilder();
          StackTraceElement[] prevStackTrace = null;
          int repeatedElements;

          if (prefixFirstLine && (multilinePrefix != null)) {
            stackBuilder.append(multilinePrefix);
          }

          do {
            if (prevStackTrace == null) {
              stackBuilder.append("Exception in thread ");
            }
            else {
              if (prefixFirstLine && (multilinePrefix != null)) {
                stackBuilder.append(multilinePrefix);
              }
              stackBuilder.append("Caused by: ");
            }

            stackBuilder.append(throwable.getClass().getCanonicalName());
            stackBuilder.append(": ");
            stackBuilder.append(throwable.getMessage());

            for (StackTraceElement singleElement : throwable.getStackTrace()) {
              if (multilinePrefix != null) {
                stackBuilder.append(multilinePrefix);
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
      case 'z':
        if ((parameters = record.getParameters()).length > 0) {

          StringBuilder parameterBuilder = new StringBuilder();
          int parameterCount = 0;

          for (Parameter parameter : parameters) {

            if ((precision > 0) && (++parameterCount > precision)) {
              break;
            }

            if ((prefixFirstLine || (parameterBuilder.length() > 0)) && (multilinePrefix != null)) {
              parameterBuilder.append(multilinePrefix);
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
      case 'p':
        if ((probeReport = record.getProbeReport()) != null) {
          return trimToWidthAndPad(probeReport.getTitle());
        }

        return null;
      case 'r':

        if ((probeReport = record.getProbeReport()) != null) {

          StringBuilder correlatorBuilder = new StringBuilder();

          if (prefixFirstLine && (multilinePrefix != null)) {
            correlatorBuilder.append(multilinePrefix);
          }

          correlatorBuilder.append("Thread Identifier: ");
          correlatorBuilder.append(probeReport.getCorrelator().getThreadIdentifier());

          if (multilinePrefix != null) {
            correlatorBuilder.append(multilinePrefix);
          }
          correlatorBuilder.append("Parent Identifier: ");
          correlatorBuilder.append(probeReport.getCorrelator().getParentIdentifier());

          if (multilinePrefix != null) {
            correlatorBuilder.append(multilinePrefix);
          }
          correlatorBuilder.append("Identifier: ");
          correlatorBuilder.append(probeReport.getCorrelator().getIdentifier());

          if (multilinePrefix != null) {
            correlatorBuilder.append(multilinePrefix);
          }
          correlatorBuilder.append("Frame: ");
          correlatorBuilder.append(probeReport.getCorrelator().getFrame());

          if (multilinePrefix != null) {
            correlatorBuilder.append(multilinePrefix);
          }
          correlatorBuilder.append("Instance: ");
          correlatorBuilder.append(probeReport.getCorrelator().getInstance());
        }

        return null;
      case 's':
        if ((probeReport = record.getProbeReport()) != null) {
          return trimToWidthAndPad(probeReport.getProbeEntry().getProbeStatus().name());
        }

        return null;
      case 'u':
        if (((probeReport = record.getProbeReport()) != null) && (probeReport.getProbeEntry() instanceof UpdateProbeEntry)) {
          return trimToWidthAndPad(String.valueOf(((UpdateProbeEntry)probeReport.getProbeEntry()).getUpdateCount()));
        }

        return null;
      case 'i':
        if (((probeReport = record.getProbeReport()) != null) && (probeReport.getProbeEntry() instanceof UpdateProbeEntry)) {
          return trimToWidthAndPad(String.valueOf(((UpdateProbeEntry)probeReport.getProbeEntry()).getUpdateTime()));
        }

        return null;
      case 'a':
        if (((probeReport = record.getProbeReport()) != null) && (probeReport.getProbeEntry() instanceof CompleteOrAbortProbeEntry)) {
          return trimToWidthAndPad(String.valueOf(((CompleteOrAbortProbeEntry)probeReport.getProbeEntry()).getStartTime()));
        }

        return null;
      case 'b':
        if (((probeReport = record.getProbeReport()) != null) && (probeReport.getProbeEntry() instanceof CompleteOrAbortProbeEntry)) {
          return trimToWidthAndPad(String.valueOf(((CompleteOrAbortProbeEntry)probeReport.getProbeEntry()).getStopTime()));
        }

        return null;
      case 'e':
        if (((probeReport = record.getProbeReport()) != null) && (probeReport.getProbeEntry() instanceof CompleteOrAbortProbeEntry)) {
          return trimToWidthAndPad(String.valueOf(((CompleteOrAbortProbeEntry)probeReport.getProbeEntry()).getStopTime() - ((CompleteOrAbortProbeEntry)probeReport.getProbeEntry()).getStartTime()));
        }

        return null;
      case 'x':
        if ((probeReport = record.getProbeReport()) != null) {

          StringBuilder statementBuilder = new StringBuilder();
          int statementCount = 0;

          for (Statement statement : probeReport.getProbeEntry().getStatements()) {

            boolean skipStatement = false;

            if ((precision > 0) && (++statementCount > precision)) {
              break;
            }

            if (!filterCollection.isEmpty()) {
              filterRecord = LoggingBlueprintsFactory.getLoggingBlueprints().filterRecord(record, statement.getDiscriminator(), statement.getLevel());
              for (Filter filter : filterCollection) {
                if (!filter.willLog(filterRecord)) {
                  skipStatement = true;
                  break;
                }
              }
            }

            if (!skipStatement) {
              if ((prefixFirstLine || (statementBuilder.length() > 0)) && (multilinePrefix != null)) {
                statementBuilder.append(multilinePrefix);
              }

              statementBuilder.append(statement.getMessage());
            }
          }

          if (statementBuilder.length() > 0) {
            return statementBuilder.toString();
          }
        }

        return null;
      case 'y':
        if ((probeReport = record.getProbeReport()) != null) {

          StringBuilder metricBuilder = new StringBuilder();
          int metricCount = 0;

          for (MetricMilieu metricMilieu : probeReport.getProbeEntry().getMetricMilieus()) {

            boolean skipMetric = false;

            if ((precision > 0) && (++metricCount > precision)) {
              break;
            }

            if (!filterCollection.isEmpty()) {
              filterRecord = LoggingBlueprintsFactory.getLoggingBlueprints().filterRecord(record, metricMilieu.getDiscriminator(), metricMilieu.getLevel());
              for (Filter filter : filterCollection) {
                if (!filter.willLog(filterRecord)) {
                  skipMetric = true;
                  break;
                }
              }
            }

            if (!skipMetric) {

              boolean listInitiated = false;

              if ((prefixFirstLine || (metricBuilder.length() > 0)) && (multilinePrefix != null)) {
                metricBuilder.append(multilinePrefix);
              }

              metricBuilder.append(metricMilieu.getMetric().getTitle());
              metricBuilder.append(" [");
              for (String key : metricMilieu.getMetric().getKeys()) {
                if (listInitiated) {
                  metricBuilder.append(", ");
                }
                listInitiated = true;

                metricBuilder.append(key);
                metricBuilder.append('=');
                metricBuilder.append(metricMilieu.getMetric().getData(key).toString());
              }
              metricBuilder.append("]");
            }
          }

          if (metricBuilder.length() > 0) {
            return metricBuilder.toString();
          }
        }

        return null;
      default:
        throw new UnknownSwitchCaseException("%c", conversion);
    }
  }

  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
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
    }
    else if (width > 0) {

      return field.substring(0, width);
    }

    return field;
  }
}