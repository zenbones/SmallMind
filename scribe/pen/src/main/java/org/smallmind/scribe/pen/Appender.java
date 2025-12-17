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
 * Destination for publishing log records.
 */
public interface Appender {

  /**
   * Returns the appender name.
   *
   * @return name or {@code null}
   */
  String getName ();

  /**
   * Sets the appender name.
   *
   * @param name name to assign
   */
  void setName (String name);

  /**
   * Convenience setter for a single filter.
   *
   * @param filter filter to install
   */
  void setFilter (Filter filter);

  /**
   * Clears all configured filters.
   */
  void clearFilters ();

  /**
   * Adds a filter that can veto records.
   *
   * @param filter filter to add
   */
  void addFilter (Filter filter);

  /**
   * Returns the current filters.
   *
   * @return array of filters
   */
  Filter[] getFilters ();

  /**
   * Replaces all filters with the supplied list.
   *
   * @param filterList filters to install
   */
  void setFilters (List<Filter> filterList);

  /**
   * Returns the configured error handler.
   *
   * @return error handler or {@code null}
   */
  ErrorHandler getErrorHandler ();

  /**
   * Sets the error handler invoked when publishing fails.
   *
   * @param errorHandler handler to use
   */
  void setErrorHandler (ErrorHandler errorHandler);

  /**
   * Indicates whether the appender will accept records.
   *
   * @return {@code true} when active
   */
  boolean isActive ();

  /**
   * Activates or deactivates the appender.
   *
   * @param active {@code true} to activate
   */
  void setActive (boolean active);

  /**
   * Publishes the supplied record.
   *
   * @param record record to publish
   */
  void publish (Record<?> record);

  /**
   * Handles an error using the configured error handler, if present.
   *
   * @param loggerName origin logger name
   * @param throwable  throwable to report
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
   * Handles an error using the configured error handler, if present.
   *
   * @param record    origin record
   * @param throwable throwable to report
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
   * Closes the appender and releases resources.
   *
   * @throws InterruptedException if shutdown is interrupted
   * @throws LoggerException      if closure fails
   */
  void close ()
    throws InterruptedException, LoggerException;
}
