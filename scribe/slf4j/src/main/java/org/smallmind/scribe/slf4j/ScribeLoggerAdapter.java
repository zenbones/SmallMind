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
 * SLF4J {@link org.slf4j.Logger} implementation that delegates to a scribe {@link Logger}.
 * This adapter translates SLF4J-style formatting and levels into the scribe API.
 */
public class ScribeLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

  private enum TranslatorState {CHAR, ESCAPE, VAR}

  private final Logger logger;

  /**
   * Constructs an adapter around the provided scribe logger.
   *
   * @param logger the underlying scribe logger to delegate to
   */
  public ScribeLoggerAdapter (Logger logger) {

    this.logger = logger;
  }

  /**
   * Returns the logger name.
   *
   * @return the name of the underlying logger
   */
  public String getName () {

    return logger.getName();
  }

  /**
   * Indicates whether TRACE level logging is enabled.
   *
   * @return {@code true} if TRACE messages should be emitted
   */
  public boolean isTraceEnabled () {

    return logger.getLevel().noGreater(Level.TRACE);
  }

  /**
   * Translates SLF4J brace-style message formats into {@link java.util.Formatter} style.
   *
   * @param format the SLF4J format string
   * @return a format string using {@code %s} placeholders
   */
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
   * Logs a TRACE message.
   *
   * @param msg the message to log
   */
  public void trace (String msg) {

    logger.trace(msg);
  }

  /**
   * Logs a TRACE message with a single argument.
   *
   * @param format SLF4J-style format string
   * @param arg1   argument to interpolate
   */
  public void trace (String format, Object arg1) {

    logger.trace(translateFormat(format), arg1);
  }

  /**
   * Logs a TRACE message with two arguments.
   *
   * @param format SLF4J-style format string
   * @param arg1   first argument to interpolate
   * @param arg2   second argument to interpolate
   */
  public void trace (String format, Object arg1, Object arg2) {

    logger.trace(translateFormat(format), arg1, arg2);
  }

  /**
   * Logs a TRACE message with an argument array.
   *
   * @param format SLF4J-style format string
   * @param args   arguments to interpolate
   */
  public void trace (String format, Object[] args) {

    logger.trace(translateFormat(format), args);
  }

  /**
   * Logs a TRACE message with an associated throwable.
   *
   * @param msg       the message to log
   * @param throwable throwable to attach
   */
  public void trace (String msg, Throwable throwable) {

    logger.trace(throwable, msg);
  }

  /**
   * Indicates whether DEBUG level logging is enabled.
   *
   * @return {@code true} if DEBUG messages should be emitted
   */
  public boolean isDebugEnabled () {

    return logger.getLevel().noGreater(Level.DEBUG);
  }

  /**
   * Logs a DEBUG message.
   *
   * @param msg the message to log
   */
  public void debug (String msg) {

    logger.debug(msg);
  }

  /**
   * Logs a DEBUG message with a single argument.
   *
   * @param format SLF4J-style format string
   * @param arg1   argument to interpolate
   */
  public void debug (String format, Object arg1) {

    logger.debug(translateFormat(format), arg1);
  }

  /**
   * Logs a DEBUG message with two arguments.
   *
   * @param format SLF4J-style format string
   * @param arg1   first argument to interpolate
   * @param arg2   second argument to interpolate
   */
  public void debug (String format, Object arg1, Object arg2) {

    logger.debug(translateFormat(format), arg1, arg2);
  }

  /**
   * Logs a DEBUG message with an argument array.
   *
   * @param format SLF4J-style format string
   * @param args   arguments to interpolate
   */
  public void debug (String format, Object[] args) {

    logger.debug(translateFormat(format), args);
  }

  /**
   * Logs a DEBUG message with an associated throwable.
   *
   * @param msg       the message to log
   * @param throwable throwable to attach
   */
  public void debug (String msg, Throwable throwable) {

    logger.debug(throwable, msg);
  }

  /**
   * Logs an INFO message.
   *
   * @param msg the message to log
   */
  public void info (String msg) {

    logger.info(msg);
  }

  /**
   * Logs an INFO message with a single argument.
   *
   * @param format SLF4J-style format string
   * @param arg1   argument to interpolate
   */
  public void info (String format, Object arg1) {

    logger.info(translateFormat(format), arg1);
  }

  /**
   * Logs an INFO message with two arguments.
   *
   * @param format SLF4J-style format string
   * @param arg1   first argument to interpolate
   * @param arg2   second argument to interpolate
   */
  public void info (String format, Object arg1, Object arg2) {

    logger.info(translateFormat(format), arg1, arg2);
  }

  /**
   * Logs an INFO message with an argument array.
   *
   * @param format SLF4J-style format string
   * @param args   arguments to interpolate
   */
  public void info (String format, Object[] args) {

    logger.info(translateFormat(format), args);
  }

  /**
   * Logs an INFO message with an associated throwable.
   *
   * @param msg       the message to log
   * @param throwable throwable to attach
   */
  public void info (String msg, Throwable throwable) {

    logger.info(throwable, msg);
  }

  /**
   * Indicates whether WARN level logging is enabled.
   *
   * @return {@code true} if WARN messages should be emitted
   */
  public boolean isWarnEnabled () {

    return logger.getLevel().noGreater(Level.WARN);
  }

  /**
   * Logs a WARN message.
   *
   * @param msg the message to log
   */
  public void warn (String msg) {

    logger.warn(msg);
  }

  /**
   * Logs a WARN message with a single argument.
   *
   * @param format SLF4J-style format string
   * @param arg1   argument to interpolate
   */
  public void warn (String format, Object arg1) {

    logger.warn(translateFormat(format), arg1);
  }

  /**
   * Logs a WARN message with two arguments.
   *
   * @param format SLF4J-style format string
   * @param arg1   first argument to interpolate
   * @param arg2   second argument to interpolate
   */
  public void warn (String format, Object arg1, Object arg2) {

    logger.warn(translateFormat(format), arg1, arg2);
  }

  /**
   * Logs a WARN message with an argument array.
   *
   * @param format SLF4J-style format string
   * @param args   arguments to interpolate
   */
  public void warn (String format, Object[] args) {

    logger.warn(translateFormat(format), args);
  }

  /**
   * Logs a WARN message with an associated throwable.
   *
   * @param msg       the message to log
   * @param throwable throwable to attach
   */
  public void warn (String msg, Throwable throwable) {

    logger.warn(throwable, msg);
  }

  /**
   * Indicates whether ERROR level logging is enabled.
   *
   * @return {@code true} if ERROR messages should be emitted
   */
  public boolean isErrorEnabled () {

    return logger.getLevel().noGreater(Level.ERROR);
  }

  /**
   * Logs an ERROR message.
   *
   * @param msg the message to log
   */
  public void error (String msg) {

    logger.error(msg);
  }

  /**
   * Logs an ERROR message with a single argument.
   *
   * @param format SLF4J-style format string
   * @param arg1   argument to interpolate
   */
  public void error (String format, Object arg1) {

    logger.error(translateFormat(format), arg1);
  }

  /**
   * Logs an ERROR message with two arguments.
   *
   * @param format SLF4J-style format string
   * @param arg1   first argument to interpolate
   * @param arg2   second argument to interpolate
   */
  public void error (String format, Object arg1, Object arg2) {

    logger.error(translateFormat(format), arg1, arg2);
  }

  /**
   * Logs an ERROR message with an argument array.
   *
   * @param format SLF4J-style format string
   * @param args   arguments to interpolate
   */
  public void error (String format, Object[] args) {

    logger.error(translateFormat(format), args);
  }

  /**
   * Logs an ERROR message with an associated throwable.
   *
   * @param msg       the message to log
   * @param throwable throwable to attach
   */
  public void error (String msg, Throwable throwable) {

    logger.error(throwable, msg);
  }

  /**
   * Indicates whether INFO level logging is enabled.
   *
   * @return {@code true} if INFO messages should be emitted
   */
  public boolean isInfoEnabled () {

    return logger.getLevel().noGreater(Level.INFO);
  }

  /**
   * Logs a message through the location-aware SLF4J API without an argument array.
   *
   * @param marker    unused marker
   * @param fqcn      fully qualified class name of the caller
   * @param level     SLF4J level constant
   * @param msg       message to log
   * @param throwable throwable to attach
   */
  public void log (Marker marker, String fqcn, int level, String msg, Throwable throwable) {

    log(marker, fqcn, level, msg, null, throwable);
  }

  /**
   * Logs a message through the location-aware SLF4J API.
   *
   * @param marker    unused marker
   * @param fqcn      fully qualified class name of the caller
   * @param level     SLF4J level constant
   * @param msg       message to log
   * @param objects   optional arguments to interpolate
   * @param throwable throwable to attach
   * @throws UnknownSwitchCaseException if the level cannot be translated
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
