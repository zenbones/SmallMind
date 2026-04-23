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
 * Formatter that serializes log records to an XML document fragment. The set of elements included,
 * the indentation depth, the newline delimiter, and whether message and parameter values are wrapped
 * in CDATA sections are all configurable via fluent setters.
 */
public class XMLFormatter implements Formatter {

  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private RecordElement[] recordElements = RecordElement.values();
  private String newLine = System.getProperty("line.separator");
  private boolean cdata = false;
  private int indent = 3;

  /**
   * Constructs an XML formatter with default settings: the default {@link DateFormatTimestamp},
   * the platform line separator, an indent of three spaces per level, CDATA disabled, and all
   * {@link RecordElement} values included.
   */
  public XMLFormatter () {

  }

  /**
   * Constructs an XML formatter with all formatting options explicitly specified.
   *
   * @param timestamp      the timestamp provider used for the {@code <date>} element
   * @param newLine        the newline string appended after each XML line (e.g. {@code "\n"})
   * @param indent         number of spaces per indentation level
   * @param cdata          {@code true} to wrap message and parameter values in {@code <![CDATA[...]]>} sections
   * @param recordElements the subset of {@link RecordElement} values to include in the output, in order
   */
  public XMLFormatter (Timestamp timestamp, String newLine, int indent, boolean cdata, RecordElement... recordElements) {

    this.timestamp = timestamp;
    this.newLine = newLine;
    this.indent = indent;
    this.cdata = cdata;
    this.recordElements = recordElements;
  }

  /**
   * Searches the previous throwable's stack trace for the given frame and returns the number of
   * remaining frames at and after the match, supporting the "... N more" abbreviation for chained
   * exceptions.
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
   * Replaces the set of record elements that will be included in formatted output.
   *
   * @param recordElements the ordered array of {@link RecordElement} values to render
   * @return this formatter, for method chaining
   */
  public XMLFormatter setRecordElements (RecordElement[] recordElements) {

    this.recordElements = recordElements;

    return this;
  }

  /**
   * Sets the timestamp provider used to render the {@code <date>} element.
   *
   * @param timestamp the timestamp provider to use
   * @return this formatter, for method chaining
   */
  public XMLFormatter setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;

    return this;
  }

  /**
   * Sets the newline string that is appended after each XML line in the output.
   *
   * @param newLine the newline delimiter (e.g. {@code "\n"} or {@code "\r\n"})
   * @return this formatter, for method chaining
   */
  public XMLFormatter setNewLine (String newLine) {

    this.newLine = newLine;

    return this;
  }

  /**
   * Controls whether message text and parameter values are wrapped in {@code <![CDATA[...]]>} sections,
   * which prevents special XML characters in log output from invalidating the document structure.
   *
   * @param cdata {@code true} to enable CDATA wrapping; {@code false} to emit values as plain text
   * @return this formatter, for method chaining
   */
  public XMLFormatter setCdata (boolean cdata) {

    this.cdata = cdata;

    return this;
  }

  /**
   * Sets the number of spaces used per indentation level in the XML output.
   *
   * @param indent the number of spaces per indentation level; zero produces no indentation
   * @return this formatter, for method chaining
   */
  public XMLFormatter setIndent (int indent) {

    this.indent = indent;

    return this;
  }

  /**
   * Serializes the given log record to an XML fragment rooted at a {@code <log-record>} element,
   * containing only the child elements specified by the configured {@link RecordElement} set. The
   * closing tag is followed by the platform line separator.
   *
   * @param record the log record to serialize
   * @return an XML string representing the record, terminated by the platform line separator
   */
  public String format (Record<?> record) {

    StringBuilder formatBuilder = new StringBuilder();

    appendLine(formatBuilder, "<log-record>", 0);

    for (RecordElement recordElement : recordElements) {
      switch (recordElement) {
        case DATE:
          appendElement(formatBuilder, "date", timestamp.getTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault())), false, 1);
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

  /**
   * Appends a {@code <thread>} element containing {@code <name>} and {@code <id>} child elements
   * to the buffer, but only when at least one of those values is present.
   *
   * @param formatBuilder the buffer being built
   * @param threadName    the thread name to include, or {@code null} if unavailable
   * @param threadId      the thread identifier to include, or a non-positive value if unavailable
   * @param level         the current indentation level
   */
  private void appendThreadInfo (StringBuilder formatBuilder, String threadName, long threadId, int level) {

    if ((threadName != null) || (threadId > 0)) {
      appendLine(formatBuilder, "<thread>", level);
      appendElement(formatBuilder, "name", threadName, false, level + 1);
      appendElement(formatBuilder, "id", (threadId > 0) ? String.valueOf(threadId) : null, false, level + 1);
      appendLine(formatBuilder, "</thread>", level);
    }
  }

  /**
   * Appends a {@code <context>} element containing class, method, native-flag, line-number, and
   * file-name child elements to the buffer. The element is omitted when the context is {@code null}
   * or not populated.
   *
   * @param formatBuilder the buffer being built
   * @param loggerContext the logger context to render, or {@code null}
   * @param level         the current indentation level
   */
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

  /**
   * Appends a {@code <parameters>} element whose children are one element per key-value parameter
   * pair. The entire block is omitted when the parameters array is empty.
   *
   * @param formatBuilder the buffer being built
   * @param parameters    the array of parameters to render; must not be {@code null}
   * @param level         the current indentation level
   */
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

  /**
   * Appends a {@code <stack-trace>} element containing the full exception chain, including
   * "... N more" abbreviations for shared frames between chained exceptions. The element is
   * optionally wrapped in a CDATA section. Nothing is appended when {@code throwable} is {@code null}.
   *
   * @param formatBuilder the buffer being built
   * @param throwable     the exception whose stack trace should be rendered, or {@code null}
   * @param level         the current indentation level
   */
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

  /**
   * Appends a single {@code <tagName>value</tagName>} element to the buffer, preceded by
   * indentation and followed by the configured newline. The element is suppressed entirely when
   * {@code value} is {@code null}.
   *
   * @param formatBuilder the buffer being built
   * @param tagName       the XML element name
   * @param value         the element text content, or {@code null} to suppress the element
   * @param includeCDATA  {@code true} to wrap the value in a {@code <![CDATA[...]]>} section
   * @param level         the current indentation level
   */
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

  /**
   * Appends the given content to the buffer, preceded by the appropriate indentation and followed
   * by the configured newline delimiter.
   *
   * @param formatBuilder the buffer being built
   * @param content       the text to append
   * @param level         the current indentation level
   */
  private void appendLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(newLine);
  }

  /**
   * Appends the given content preceded by indentation and followed by the platform line separator
   * (rather than the configured newline). Used for the closing {@code </log-record>} tag so that
   * sequential records remain separated by a consistent system-specific newline.
   *
   * @param formatBuilder the buffer being built
   * @param content       the text to append
   * @param level         the current indentation level
   */
  private void appendFinalLine (StringBuilder formatBuilder, String content, int level) {

    appendIndent(formatBuilder, level);
    formatBuilder.append(content);
    formatBuilder.append(System.getProperty("line.separator"));
  }

  /**
   * Appends the correct number of leading spaces for the given indentation level to the buffer.
   * The number of spaces is {@code level * indent} where {@code indent} is the configured spaces-per-level.
   *
   * @param formatBuilder the buffer being built
   * @param level         the indentation level; level zero produces no leading spaces
   */
  private void appendIndent (StringBuilder formatBuilder, int level) {

    formatBuilder.append(" ".repeat(level * indent));
  }
}
