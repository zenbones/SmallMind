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

import java.util.List;

/**
 * Named, filterable destination that receives {@link Record} objects and writes them to an output target such as a
 * file, console, or remote sink.
 */
public interface Appender {

  /**
   * Returns the name assigned to this appender.
   *
   * @return the appender name, or {@code null} if none has been set
   */
  String getName ();

  /**
   * Assigns a human-readable name to this appender.
   *
   * @param name the name to assign
   */
  void setName (String name);

  /**
   * Replaces the entire filter list with a single filter, discarding any previously configured filters.
   *
   * @param filter the sole filter to install
   */
  void setFilter (Filter filter);

  /**
   * Removes all filters from this appender so that every record passes through unconditionally.
   */
  void clearFilters ();

  /**
   * Appends a filter to the existing filter chain; records must be accepted by every filter to be published.
   *
   * @param filter the filter to add
   */
  void addFilter (Filter filter);

  /**
   * Returns all filters currently installed on this appender.
   *
   * @return an array of filters, never {@code null}
   */
  Filter[] getFilters ();

  /**
   * Replaces all currently installed filters with the contents of the supplied list.
   *
   * @param filterList the filters to install; {@code null} elements are ignored by implementations
   */
  void setFilters (List<Filter> filterList);

  /**
   * Returns the error handler that is invoked when a publishing failure occurs.
   *
   * @return the configured {@link ErrorHandler}, or {@code null} if none is set
   */
  ErrorHandler getErrorHandler ();

  /**
   * Sets the error handler to be invoked when this appender encounters a publishing failure.
   *
   * @param errorHandler the handler to use; may be {@code null} to revert to default stack-trace behaviour
   */
  void setErrorHandler (ErrorHandler errorHandler);

  /**
   * Returns whether this appender is currently accepting and publishing records.
   *
   * @return {@code true} if the appender is active and will process records
   */
  boolean isActive ();

  /**
   * Enables or disables record processing for this appender.
   *
   * @param active {@code true} to activate the appender, {@code false} to silence it
   */
  void setActive (boolean active);

  /**
   * Publishes the supplied record to this appender's output target after passing it through all installed filters.
   *
   * @param record the log record to publish
   */
  void publish (Record<?> record);

  /**
   * Reports a logger-level error to the configured {@link ErrorHandler}, or prints the stack trace if no handler is
   * set.
   *
   * @param loggerName the name of the logger where the error originated
   * @param throwable  the failure to report
   */
  default void handleError (String loggerName, Throwable throwable) {

    ErrorHandler errorHandler;

    if ((errorHandler = getErrorHandler()) == null) {
      throwable.printStackTrace();
    } else {
      errorHandler.process(loggerName, throwable, "Error in appender(%s)", (getName() != null) ? getName() : this.getClass().getCanonicalName());
    }
  }

  /**
   * Reports a record-level publishing error to the configured {@link ErrorHandler}, or prints the stack trace if no
   * handler is set.
   *
   * @param record    the record that was being processed when the error occurred
   * @param throwable the failure to report
   */
  default void handleError (Record<?> record, Throwable throwable) {

    ErrorHandler errorHandler;

    if ((errorHandler = getErrorHandler()) == null) {
      throwable.printStackTrace();
    } else {
      errorHandler.process(record, throwable, "Publishing error in appender(%s)", (getName() != null) ? getName() : this.getClass().getCanonicalName());
    }
  }

  /**
   * Shuts down this appender, flushing any buffered output and releasing all held resources.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for shutdown to complete
   * @throws LoggerException      if an error occurs during closure
   */
  void close ()
    throws InterruptedException, LoggerException;
}
