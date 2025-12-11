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

public class XMLFormatter implements Formatter {

  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private RecordElement[] recordElements = RecordElement.values();
  private String newLine = System.getProperty("line.separator");
  private boolean cdata = false;
  private int indent = 3;

  public XMLFormatter () {

  }

  public XMLFormatter (Timestamp timestamp, String newLine, int indent, boolean cdata, RecordElement... recordElements) {

    this.timestamp = timestamp;
    this.newLine = newLine;
    this.indent = indent;
    this.cdata = cdata;
    this.recordElements = recordElements;
  }

  private static int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
  }

  public XMLFormatter setRecordElements (RecordElement[] recordElements) {

    this.recordElements = recordElements;

    return this;
  }

  public XMLFormatter setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;

    return this;
  }

  public XMLFormatter setNewLine (String newLine) {

    this.newLine = newLine;

    return this;
  }

  public XMLFormatter setCdata (boolean cdata) {

    this.cdata = cdata;

    return this;
  }

  public XMLFormatter setIndent (int indent) {

    this.indent = indent;

    return this;
  }

  public String format (Record<?> record) {

    StringBuilder formatBuilder = new StringBuilder();

    appendLine(formatBuilder, "<log-record>", 0);

    for (RecordElement recordElement : recordElements) {
      switch (recordElement) {
        case DATE:
          appendElement(formatBuilder, "date", timestamp.getTimestamp(new Date(record.getMillis())), false, 1);
          break;
        case MILLISECONDS:
          appendElement(formatBuilder, "milliseconds", String.valueOf(record.getMillis()), false, 1);
          break;
        case LOGGER_NAME:
          appendElement(formatBuilder, "logger", record.getLoggerName(), false, 1);
          break;
        case LEVEL:
          appendElement(formatBuilder, "level", record.getLevel().name(), false, 1);
          break;
        case MESSAGE:

          String message;

          if ((message = record.getMessage()) == null) {

            Throwable throwable;

            if ((throwable = record.getThrown()) != null) {
              message = throwable.getMessage();
            }
          }

          appendElement(formatBuilder, "message", message, cdata, 1);
          break;
        case THREAD:
          appendThreadInfo(formatBuilder, record.getThreadName(), record.getThreadID(), 1);
          break;
        case LOGGER_CONTEXT:
          appendLoggerContext(formatBuilder, record.getLoggerContext(), 1);
          break;
        case PARAMETERS:
          appendParameters(formatBuilder, record.getParameters(), 1);
          break;
        case STACK_TRACE:
          appendStackTrace(formatBuilder, record.getThrown(), 1);
          break;
        default:
          throw new UnknownSwitchCaseException(recordElement.name());
      }
    }

    appendFinalLine(formatBuilder, "</log-record>", 0);

    return formatBuilder.toString();
  }

  private void appendThreadInfo (StringBuilder formatBuilder, String threadName, long threadId, int level) {

    if ((threadName != null) || (threadId > 0)) {
      appendLine(formatBuilder, "<thread>", level);
      appendElement(formatBuilder, "name", threadName, false, level + 1);
      appendElement(formatBuilder, "id", (threadId > 0) ? String.valueOf(threadId) : null, false, level + 1);
      appendLine(formatBuilder, "</thread>", level);
    }
  }

  private void appendLoggerContext (StringBuilder formatBuilder, LoggerContext loggerContext, int level) {

    if ((loggerContext != null) && (loggerContext.isFilled())) {
      appendLine(formatBuilder, "<context>", level);
      appendElement(formatBuilder, "class", loggerContext.getClassName(), false, level + 1);
      appendElement(formatBuilder, "method", loggerContext.getMethodName(), false, level + 1);
      appendElement(formatBuilder, "native", String.valueOf(loggerContext.isNativeMethod()), false, level + 1);
      appendElement(formatBuilder, "line", ((!loggerContext.isNativeMethod()) && (loggerContext.getLineNumber() > 0)) ? String.valueOf(loggerContext.getLineNumber()) : null, false, level + 1);
      appendElement(formatBuilder, "file", loggerContext.getFileName(), false, level + 1);
      appendLine(formatBuilder, "</context>", level);
    }
  }

  private void appendParameters (StringBuilder formatBuilder, Parameter[] parameters, int level) {

    if (parameters.length > 0) {
      appendLine(formatBuilder, "<parameters>", level);
      for (Parameter parameter : parameters) {

        String key = parameter.getKey();
        Object value = parameter.getValue();

        appendElement(formatBuilder, (key == null) ? "null" : key, (value == null) ? "null" : value.toString(), cdata, level + 1);
      }
      appendLine(formatBuilder, "</parameters>", level);
    }
  }

  private void appendStackTrace (StringBuilder formatBuilder, Throwable throwable, int level) {

    StackTraceElement[] prevStackTrace = null;
    int repeatedElements;

    if (throwable != null) {
      appendLine(formatBuilder, (cdata) ? "<stack-trace><![CDATA[" : "<stack-trace>", level);

      do {
        appendIndent(formatBuilder, level + 1);

        if (prevStackTrace == null) {
          formatBuilder.append("Exception in thread ");
        } else {
          formatBuilder.append("Caused by: ");
        }

        formatBuilder.append(throwable.getClass().getCanonicalName());
        formatBuilder.append(": ");
        formatBuilder.append(throwable.getMessage());
        formatBuilder.append(newLine);

        for (StackTraceElement singleElement : throwable.getStackTrace()) {

          appendIndent(formatBuilder, level + 1);

          if (prevStackTrace != null) {
            if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
              formatBuilder.append("   ... ");
              formatBuilder.append(repeatedElements);
              formatBuilder.append(" more");
              formatBuilder.append(newLine);
              break;
            }
          }

          formatBuilder.append("   at ");
          formatBuilder.append(singleElement.toString());
          formatBuilder.append(newLine);
        }

        prevStackTrace = throwable.getStackTrace();
      } while ((throwable = throwable.getCause()) != null);

      appendLine(formatBuilder, (cdata) ? "]]></stack-trace>" : "</stack-trace>", level);
    }
  }

  private void appendElement (StringBuilder formatBuilder, String tagName, String value, boolean includeCDATA, int level) {

    if (value != null) {
      appendIndent(formatBuilder, level);
      formatBuilder.append("<");
      formatBuilder.append(tagName);
      formatBuilder.append(">");

      if (includeCDATA) {
        formatBuilder.append("<![CDATA[");
      }

      formatBuilder.append(value);

      if (includeCDATA) {
        formatBuilder.append("]]>");
      }

      formatBuilder.append("</");
      formatBuilder.append(tagName);
      formatBuilder.append(">");
      formatBuilder.append(newLine);
    }
  }

  private void appendLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(newLine);
  }

  private void appendFinalLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(System.getProperty("line.separator"));
  }

  private void appendIndent (StringBuilder formatBuilder, int level) {

    formatBuilder.append(" ".repeat(level * indent));
  }
}