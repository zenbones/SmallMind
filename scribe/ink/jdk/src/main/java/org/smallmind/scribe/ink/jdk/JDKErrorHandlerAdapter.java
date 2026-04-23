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

import java.util.logging.ErrorManager;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Record;

/**
 * Scribe {@link ErrorHandler} that delegates error reporting to a JUL {@link ErrorManager},
 * printing the stack trace directly when the throwable is not an {@link Exception}.
 */
public class JDKErrorHandlerAdapter implements ErrorHandler {

  private final ErrorManager errorManager;

  /**
   * Builds an adapter that delegates error reporting to the given JUL {@link ErrorManager}.
   *
   * @param errorManager the native JUL error manager to delegate to
   */
  public JDKErrorHandlerAdapter (ErrorManager errorManager) {

    this.errorManager = errorManager;
  }

  /**
   * Returns the JUL {@link ErrorManager} that this adapter wraps.
   *
   * @return the native error manager
   */
  public ErrorManager getNativeErrorManager () {

    return errorManager;
  }

  /**
   * Reports an error from the named logger to the native {@link ErrorManager}; if the throwable is
   * not an {@link Exception}, its stack trace is printed directly because JUL only accepts exceptions.
   *
   * @param loggerName   the name of the logger where the error originated
   * @param throwable    the throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments substituted into the message template
   */
  @Override
  public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

    if (throwable instanceof Exception) {
      errorManager.error(MessageTranslator.translateMessage(errorMessage, args), (Exception)throwable, 0);
    } else {
      throwable.printStackTrace();
    }
  }

  /**
   * Reports an error associated with a record by extracting the logger name from the record and
   * delegating to {@link #process(String, Throwable, String, Object...)}.
   *
   * @param record       the record associated with the error
   * @param throwable    the throwable to report
   * @param errorMessage message template describing the error
   * @param args         arguments substituted into the message template
   */
  @Override
  public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

    process(record.getLoggerName(), throwable, errorMessage, args);
  }
}
