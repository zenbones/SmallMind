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
package org.smallmind.scribe.ink.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Record;

/**
 * Scribe {@link ErrorHandler} that delegates error reporting to a Log4j2
 * {@link org.apache.logging.log4j.core.ErrorHandler}, passing the native {@link LogEvent} when
 * a record is available or {@code null} when only a logger name is provided.
 */
public class Log4JErrorHandlerAdapter implements ErrorHandler {

  private final org.apache.logging.log4j.core.ErrorHandler errorHandler;

  /**
   * Builds an adapter that delegates error reporting to the given Log4j2 error handler.
   *
   * @param errorHandler the native Log4j2 error handler to delegate to
   */
  public Log4JErrorHandlerAdapter (org.apache.logging.log4j.core.ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  /**
   * Returns the native Log4j2 error handler that this adapter wraps.
   *
   * @return the wrapped Log4j2 error handler
   */
  public org.apache.logging.log4j.core.ErrorHandler getNativeErrorHandler () {

    return errorHandler;
  }

  /**
   * Reports an error from the named logger to the native Log4j2 error handler with a {@code null}
   * event reference since no record is available.
   *
   * @param loggerName   the name of the logger where the error originated
   * @param throwable    the throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments substituted into the message template
   */
  @Override
  public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

    errorHandler.error(MessageTranslator.translateMessage(errorMessage, args), null, throwable);
  }

  /**
   * Reports an error associated with a record to the native Log4j2 error handler, casting the
   * record's native log entry to a {@link LogEvent} and passing it alongside the translated message.
   *
   * @param record       the record associated with the error
   * @param throwable    the throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments substituted into the message template
   */
  @Override
  public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

    errorHandler.error(MessageTranslator.translateMessage(errorMessage, args), (LogEvent)record.getNativeLogEntry(), throwable);
  }
}
