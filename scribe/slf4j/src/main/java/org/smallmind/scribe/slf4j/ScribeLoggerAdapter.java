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
package org.smallmind.scribe.slf4j;

import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.spi.LocationAwareLogger;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

/**
 * SLF4J {@link org.slf4j.Logger} adapter backed by a scribe {@link Logger}; extends
 * {@link MarkerIgnoringBase} so that all {@code Marker}-bearing overloads are silently discarded,
 * and implements {@link LocationAwareLogger} so that bridging frameworks can supply caller
 * location information. SLF4J {@code {}} placeholders in format strings are rewritten to
 * {@code %s} before the call is forwarded to the scribe pipeline.
 */
public class ScribeLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

  private enum TranslatorState {CHAR, ESCAPE, VAR}

  private final Logger logger;

  /**
   * Constructs an adapter that delegates all log calls to the given scribe logger.
   *
   * @param logger the scribe logger that will handle every forwarded event
   */
  public ScribeLoggerAdapter (Logger logger) {

    this.logger = logger;
  }

  /**
   * Returns the name of the underlying scribe logger.
   *
   * @return the logger name
   */
  public String getName () {

    return logger.getName();
  }

  /**
   * Reports whether TRACE-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is TRACE or finer
   */
  public boolean isTraceEnabled () {

    return logger.getLevel().noGreater(Level.TRACE);
  }

  private String translateFormat (String format) {

    StringBuilder formatBuilder = new StringBuilder();
    TranslatorState state = TranslatorState.CHAR;

    for (int index = 0; index < format.length(); index++) {

      char currentChar = format.charAt(index);

      switch (state) {
        case CHAR:
          if (currentChar == '\\') {
            state = TranslatorState.ESCAPE;
          } else if (currentChar == '{') {
            state = TranslatorState.VAR;
          } else {
            formatBuilder.append(currentChar);
          }
          break;
        case ESCAPE:
          formatBuilder.append(currentChar);
          state = TranslatorState.CHAR;
          break;
        case VAR:
          if (currentChar == '{') {
            formatBuilder.append('{');
          } else {
            if (currentChar == '}') {
              formatBuilder.append("%s");
            } else {
              formatBuilder.append('{').append(currentChar);
            }
            state = TranslatorState.CHAR;
          }
          break;
        default:
          throw new UnknownSwitchCaseException(state.name());
      }
    }

    return formatBuilder.toString();
  }

  /**
   * Emits a TRACE-level event with the given literal message.
   *
   * @param msg the message string to log
   */
  public void trace (String msg) {

    logger.trace(msg);
  }

  /**
   * Emits a TRACE-level event, translating the SLF4J {@code {}} placeholder in {@code format}
   * to {@code %s} before interpolating {@code arg1}.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   */
  public void trace (String format, Object arg1) {

    logger.trace(translateFormat(format), arg1);
  }

  /**
   * Emits a TRACE-level event with two interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   * @param arg2   argument substituted for the second {@code {}} placeholder
   */
  public void trace (String format, Object arg1, Object arg2) {

    logger.trace(translateFormat(format), arg1, arg2);
  }

  /**
   * Emits a TRACE-level event with an array of interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param args   arguments substituted for each {@code {}} placeholder in order
   */
  public void trace (String format, Object[] args) {

    logger.trace(translateFormat(format), args);
  }

  /**
   * Emits a TRACE-level event with an attached throwable.
   *
   * @param msg       the message string to log
   * @param throwable exception or error to attach to the event
   */
  public void trace (String msg, Throwable throwable) {

    logger.trace(throwable, msg);
  }

  /**
   * Reports whether DEBUG-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is DEBUG or finer
   */
  public boolean isDebugEnabled () {

    return logger.getLevel().noGreater(Level.DEBUG);
  }

  /**
   * Emits a DEBUG-level event with the given literal message.
   *
   * @param msg the message string to log
   */
  public void debug (String msg) {

    logger.debug(msg);
  }

  /**
   * Emits a DEBUG-level event with one interpolated argument.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   */
  public void debug (String format, Object arg1) {

    logger.debug(translateFormat(format), arg1);
  }

  /**
   * Emits a DEBUG-level event with two interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   * @param arg2   argument substituted for the second {@code {}} placeholder
   */
  public void debug (String format, Object arg1, Object arg2) {

    logger.debug(translateFormat(format), arg1, arg2);
  }

  /**
   * Emits a DEBUG-level event with an array of interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param args   arguments substituted for each {@code {}} placeholder in order
   */
  public void debug (String format, Object[] args) {

    logger.debug(translateFormat(format), args);
  }

  /**
   * Emits a DEBUG-level event with an attached throwable.
   *
   * @param msg       the message string to log
   * @param throwable exception or error to attach to the event
   */
  public void debug (String msg, Throwable throwable) {

    logger.debug(throwable, msg);
  }

  /**
   * Emits an INFO-level event with the given literal message.
   *
   * @param msg the message string to log
   */
  public void info (String msg) {

    logger.info(msg);
  }

  /**
   * Emits an INFO-level event with one interpolated argument.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   */
  public void info (String format, Object arg1) {

    logger.info(translateFormat(format), arg1);
  }

  /**
   * Emits an INFO-level event with two interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   * @param arg2   argument substituted for the second {@code {}} placeholder
   */
  public void info (String format, Object arg1, Object arg2) {

    logger.info(translateFormat(format), arg1, arg2);
  }

  /**
   * Emits an INFO-level event with an array of interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param args   arguments substituted for each {@code {}} placeholder in order
   */
  public void info (String format, Object[] args) {

    logger.info(translateFormat(format), args);
  }

  /**
   * Emits an INFO-level event with an attached throwable.
   *
   * @param msg       the message string to log
   * @param throwable exception or error to attach to the event
   */
  public void info (String msg, Throwable throwable) {

    logger.info(throwable, msg);
  }

  /**
   * Reports whether WARN-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is WARN or finer
   */
  public boolean isWarnEnabled () {

    return logger.getLevel().noGreater(Level.WARN);
  }

  /**
   * Emits a WARN-level event with the given literal message.
   *
   * @param msg the message string to log
   */
  public void warn (String msg) {

    logger.warn(msg);
  }

  /**
   * Emits a WARN-level event with one interpolated argument.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   */
  public void warn (String format, Object arg1) {

    logger.warn(translateFormat(format), arg1);
  }

  /**
   * Emits a WARN-level event with two interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   * @param arg2   argument substituted for the second {@code {}} placeholder
   */
  public void warn (String format, Object arg1, Object arg2) {

    logger.warn(translateFormat(format), arg1, arg2);
  }

  /**
   * Emits a WARN-level event with an array of interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param args   arguments substituted for each {@code {}} placeholder in order
   */
  public void warn (String format, Object[] args) {

    logger.warn(translateFormat(format), args);
  }

  /**
   * Emits a WARN-level event with an attached throwable.
   *
   * @param msg       the message string to log
   * @param throwable exception or error to attach to the event
   */
  public void warn (String msg, Throwable throwable) {

    logger.warn(throwable, msg);
  }

  /**
   * Reports whether ERROR-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is ERROR or finer
   */
  public boolean isErrorEnabled () {

    return logger.getLevel().noGreater(Level.ERROR);
  }

  /**
   * Emits an ERROR-level event with the given literal message.
   *
   * @param msg the message string to log
   */
  public void error (String msg) {

    logger.error(msg);
  }

  /**
   * Emits an ERROR-level event with one interpolated argument.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   */
  public void error (String format, Object arg1) {

    logger.error(translateFormat(format), arg1);
  }

  /**
   * Emits an ERROR-level event with two interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param arg1   argument substituted for the first {@code {}} placeholder
   * @param arg2   argument substituted for the second {@code {}} placeholder
   */
  public void error (String format, Object arg1, Object arg2) {

    logger.error(translateFormat(format), arg1, arg2);
  }

  /**
   * Emits an ERROR-level event with an array of interpolated arguments.
   *
   * @param format SLF4J brace-delimited format string
   * @param args   arguments substituted for each {@code {}} placeholder in order
   */
  public void error (String format, Object[] args) {

    logger.error(translateFormat(format), args);
  }

  /**
   * Emits an ERROR-level event with an attached throwable.
   *
   * @param msg       the message string to log
   * @param throwable exception or error to attach to the event
   */
  public void error (String msg, Throwable throwable) {

    logger.error(throwable, msg);
  }

  /**
   * Reports whether INFO-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is INFO or finer
   */
  public boolean isInfoEnabled () {

    return logger.getLevel().noGreater(Level.INFO);
  }

  /**
   * Delegates to the six-argument {@link LocationAwareLogger} overload with a {@code null}
   * argument array; provided for callers that have no format arguments.
   *
   * @param marker    ignored (marker support is not implemented)
   * @param fqcn      fully-qualified class name of the calling class, used for location resolution
   * @param level     SLF4J integer level constant (e.g. {@link LocationAwareLogger#INFO_INT})
   * @param msg       the message to log
   * @param throwable exception or error to attach, or {@code null}
   */
  public void log (Marker marker, String fqcn, int level, String msg, Throwable throwable) {

    log(marker, fqcn, level, msg, null, throwable);
  }

  /**
   * Translates the SLF4J integer level constant to a scribe {@link Level} and forwards the event.
   * This method is called by bridging frameworks (e.g. jcl-over-slf4j) that supply caller location.
   *
   * @param marker    ignored (marker support is not implemented)
   * @param fqcn      fully-qualified class name of the calling class, used for location resolution
   * @param level     SLF4J integer level constant (e.g. {@link LocationAwareLogger#INFO_INT})
   * @param msg       the message to log
   * @param objects   arguments substituted into the message, or {@code null}
   * @param throwable exception or error to attach, or {@code null}
   * @throws UnknownSwitchCaseException if {@code level} does not map to a known scribe level
   */
  public void log (Marker marker, String fqcn, int level, String msg, Object[] objects, Throwable throwable) {

    Level scribeLevel;

    switch (level) {
      case LocationAwareLogger.TRACE_INT:
        scribeLevel = Level.TRACE;
        break;
      case LocationAwareLogger.DEBUG_INT:
        scribeLevel = Level.DEBUG;
        break;
      case LocationAwareLogger.INFO_INT:
        scribeLevel = Level.INFO;
        break;
      case LocationAwareLogger.WARN_INT:
        scribeLevel = Level.WARN;
        break;
      case LocationAwareLogger.ERROR_INT:
        scribeLevel = Level.ERROR;
        break;
      default:
        throw new UnknownSwitchCaseException(String.valueOf(level));
    }

    logger.log(scribeLevel, throwable, msg, objects);
  }
}
