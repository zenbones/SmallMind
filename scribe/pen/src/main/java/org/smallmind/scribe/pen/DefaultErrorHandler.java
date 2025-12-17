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

import org.smallmind.scribe.pen.adapter.LoggingBlueprintFactory;

/**
 * Default {@link ErrorHandler} that writes errors through a backup appender (console/XML by default).
 */
public class DefaultErrorHandler implements ErrorHandler {

  private Appender appender;

  /**
   * Creates a handler that logs errors to console using {@link XMLFormatter}.
   */
  public DefaultErrorHandler () {

    appender = new ConsoleAppender(new XMLFormatter());
  }

  /**
   * Creates a handler that logs errors to the provided backup appender.
   *
   * @param appender backup appender to use
   */
  public DefaultErrorHandler (Appender appender) {

    this.appender = appender;
  }

  /**
   * Sets the backup appender used for error reporting.
   *
   * @param appender backup appender
   */
  public void setBackupAppender (Appender appender) {

    this.appender = appender;
  }

  /**
   * Logs an error using the backup appender by creating a new record from the given details.
   *
   * @param loggerName   name of the logger that failed
   * @param throwable    exception encountered, may be {@code null}
   * @param errorMessage error message template
   * @param args         optional template arguments
   */
  @Override
  public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

    appender.publish(LoggingBlueprintFactory.getLoggingBlueprint().errorRecord(loggerName, throwable, errorMessage, args));
  }

  /**
   * Logs an error related to an existing record and republishes that record via the backup appender.
   *
   * @param record       the record associated with the failure
   * @param throwable    exception encountered, may be {@code null}
   * @param errorMessage error message template
   * @param args         optional template arguments
   */
  @Override
  public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

    process(record.getLoggerName(), throwable, errorMessage, args);
    appender.publish(record);
  }
}
