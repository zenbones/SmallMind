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
 * Adapts a Log4j2 {@link org.apache.logging.log4j.core.ErrorHandler} to the scribe {@link ErrorHandler} contract.
 */
public class Log4JErrorHandlerAdapter implements ErrorHandler {

  private final org.apache.logging.log4j.core.ErrorHandler errorHandler;

  /**
   * Creates an adapter around the provided Log4j2 error handler.
   *
   * @param errorHandler native error handler
   */
  public Log4JErrorHandlerAdapter (org.apache.logging.log4j.core.ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  /**
   * Returns the wrapped Log4j2 error handler.
   *
   * @return the native error handler
   */
  public org.apache.logging.log4j.core.ErrorHandler getNativeErrorHandler () {

    return errorHandler;
  }

  /**
   * Handles an error originating from a logger.
   *
   * @param loggerName   name of the logger that produced the error
   * @param throwable    throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments applied to the message template
   */
  @Override
  public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

    errorHandler.error(MessageTranslator.translateMessage(errorMessage, args), null, throwable);
  }

  /**
   * Handles an error originating from a record, delegating to the Log4j2 handler with the native event.
   *
   * @param record       record that triggered the error handling
   * @param throwable    throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments applied to the message template
   */
  @Override
  public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

    errorHandler.error(MessageTranslator.translateMessage(errorMessage, args), (LogEvent)record.getNativeLogEntry(), throwable);
  }
}
