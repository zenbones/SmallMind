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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprint;

/**
 * {@link LoggingBlueprint} that bridges scribe to Java Util Logging; registers the
 * {@code "java.util.logging."} package prefix at class-load time and manufactures
 * {@link JDKLoggerAdapter} instances backed by named JUL loggers.
 */
public class JDKLoggingBlueprint extends LoggingBlueprint<LogRecord> {

  static {

    LoggerManager.addLoggingPackagePrefix("java.util.logging.");
  }

  /**
   * Returns a new {@link JDKLoggerAdapter} wrapping the JUL logger retrieved by the given name.
   *
   * @param name the logger name
   * @return a fresh adapter backed by the named JUL logger
   */
  @Override
  public LoggerAdapter getLoggingAdapter (String name) {

    return new JDKLoggerAdapter(Logger.getLogger(name));
  }

  /**
   * Constructs a FATAL-level {@link JDKRecordSubverter} for error reporting, filling the logger context
   * with caller information before returning the wrapped scribe record.
   *
   * @param loggerName the name of the logger from which the error originates
   * @param throwable  the throwable to attach to the record
   * @param message    message template describing the error condition
   * @param args       arguments substituted into the message template
   * @return a fully populated scribe record ready for delivery to appenders
   */
  @Override
  public Record<LogRecord> errorRecord (String loggerName, Throwable throwable, String message, Object... args) {

    LoggerContext loggerContext;

    loggerContext = new DefaultLoggerContext();
    loggerContext.fillIn();

    return new JDKRecordSubverter(loggerName, Level.FATAL, loggerContext, throwable, message, args).getRecord();
  }
}
