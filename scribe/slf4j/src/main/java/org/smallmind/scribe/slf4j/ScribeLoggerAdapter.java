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
package org.smallmind.scribe.slf4j;

import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.spi.LocationAwareLogger;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class ScribeLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

  private enum TranslatorState {CHAR, ESCAPE, VAR}

  private final Logger logger;

  public ScribeLoggerAdapter (Logger logger) {

    this.logger = logger;
  }

  public String getName () {

    return logger.getName();
  }

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

  public void trace (String msg) {

    logger.trace(msg);
  }

  public void trace (String format, Object arg1) {

    logger.trace(translateFormat(format), arg1);
  }

  public void trace (String format, Object arg1, Object arg2) {

    logger.trace(translateFormat(format), arg1, arg2);
  }

  public void trace (String format, Object[] args) {

    logger.trace(translateFormat(format), args);
  }

  public void trace (String msg, Throwable throwable) {

    logger.trace(throwable, msg);
  }

  public boolean isDebugEnabled () {

    return logger.getLevel().noGreater(Level.DEBUG);
  }

  public void debug (String msg) {

    logger.debug(msg);
  }

  public void debug (String format, Object arg1) {

    logger.debug(translateFormat(format), arg1);
  }

  public void debug (String format, Object arg1, Object arg2) {

    logger.debug(translateFormat(format), arg1, arg2);
  }

  public void debug (String format, Object[] args) {

    logger.debug(translateFormat(format), args);
  }

  public void debug (String msg, Throwable throwable) {

    logger.debug(throwable, msg);
  }

  public void info (String msg) {

    logger.info(msg);
  }

  public void info (String format, Object arg1) {

    logger.info(translateFormat(format), arg1);
  }

  public void info (String format, Object arg1, Object arg2) {

    logger.info(translateFormat(format), arg1, arg2);
  }

  public void info (String format, Object[] args) {

    logger.info(translateFormat(format), args);
  }

  public void info (String msg, Throwable throwable) {

    logger.info(throwable, msg);
  }

  public boolean isWarnEnabled () {

    return logger.getLevel().noGreater(Level.WARN);
  }

  public void warn (String msg) {

    logger.warn(msg);
  }

  public void warn (String format, Object arg1) {

    logger.warn(translateFormat(format), arg1);
  }

  public void warn (String format, Object arg1, Object arg2) {

    logger.warn(translateFormat(format), arg1, arg2);
  }

  public void warn (String format, Object[] args) {

    logger.warn(translateFormat(format), args);
  }

  public void warn (String msg, Throwable throwable) {

    logger.warn(throwable, msg);
  }

  public boolean isErrorEnabled () {

    return logger.getLevel().noGreater(Level.ERROR);
  }

  public void error (String msg) {

    logger.error(msg);
  }

  public void error (String format, Object arg1) {

    logger.error(translateFormat(format), arg1);
  }

  public void error (String format, Object arg1, Object arg2) {

    logger.error(translateFormat(format), arg1, arg2);
  }

  public void error (String format, Object[] args) {

    logger.error(translateFormat(format), args);
  }

  public void error (String msg, Throwable throwable) {

    logger.error(throwable, msg);
  }

  public boolean isInfoEnabled () {

    return logger.getLevel().noGreater(Level.INFO);
  }

  public void log (Marker marker, String fqcn, int level, String msg, Throwable throwable) {

    log(marker, fqcn, level, msg, null, throwable);
  }

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