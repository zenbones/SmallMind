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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Base implementation of an {@link Appender} that manages filters, error handling, and active state.
 * Subclasses need only implement {@link #handleOutput(Record)} to emit records.
 */
public abstract class AbstractAppender implements Appender {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private ErrorHandler errorHandler;
  private String name;
  private boolean active = true;

  /**
   * Constructs an appender without a name or error handler.
   */
  public AbstractAppender () {

    this(null, null);
  }

  /**
   * Constructs an appender with the given error handler.
   *
   * @param errorHandler handler to invoke on failures
   */
  public AbstractAppender (ErrorHandler errorHandler) {

    this(null, errorHandler);
  }

  /**
   * Constructs an appender with a name and error handler.
   *
   * @param name         appender name
   * @param errorHandler handler to invoke on failures
   */
  public AbstractAppender (String name, ErrorHandler errorHandler) {

    this.name = name;
    this.errorHandler = errorHandler;

    filterList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Emits the record to the concrete output target.
   *
   * @param record record to output
   * @throws Exception if writing fails
   */
  public abstract void handleOutput (Record<?> record)
    throws Exception;

  /**
   * Retrieves the configured name of this appender.
   *
   * @return appender name, or {@code null} if unnamed
   */
  @Override
  public String getName () {

    return name;
  }

  /**
   * Assigns a name to the appender for identification in logs or configuration.
   *
   * @param name new appender name, may be {@code null}
   */
  @Override
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Removes all configured filters, allowing all records to proceed to output.
   */
  @Override
  public synchronized void clearFilters () {

    filterList.clear();
  }

  /**
   * Replaces existing filters with a single filter.
   *
   * @param filter filter that must approve records before output
   */
  @Override
  public synchronized void setFilter (Filter filter) {

    filterList.clear();
    filterList.add(filter);
  }

  /**
   * Adds an additional filter that must approve records before output.
   *
   * @param filter filter to append to the evaluation chain
   */
  @Override
  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);
  }

  /**
   * Returns the filters currently applied to records.
   *
   * @return array of configured filters in evaluation order
   */
  @Override
  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  /**
   * Replaces all configured filters with the supplied list.
   *
   * @param replacementFilterList filters to evaluate in order
   */
  @Override
  public synchronized void setFilters (List<Filter> replacementFilterList) {

    filterList.clear();
    filterList.addAll(replacementFilterList);
  }

  /**
   * Retrieves the error handler invoked when output fails.
   *
   * @return configured error handler, or {@code null} if none
   */
  @Override
  public ErrorHandler getErrorHandler () {

    return errorHandler;
  }

  /**
   * Sets the error handler to use when publishing fails.
   *
   * @param errorHandler handler to receive failures, may be {@code null}
   */
  @Override
  public void setErrorHandler (ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  /**
   * Indicates whether this appender currently accepts records.
   *
   * @return {@code true} if active and publishing, otherwise {@code false}
   */
  @Override
  public boolean isActive () {

    return active;
  }

  /**
   * Enables or disables this appender.
   *
   * @param active {@code true} to allow publishing, {@code false} to ignore records
   */
  @Override
  public void setActive (boolean active) {

    this.active = active;
  }

  /**
   * Processes a record by applying filters and delegating output.
   * If any filter vetoes the record, output is skipped. Exceptions during output
   * are forwarded to the configured error handler.
   *
   * @param record record to publish
   */
  @Override
  public void publish (Record<?> record) {

    try {
      for (Filter filter : filterList) {
        if (!filter.willLog(record)) {
          return;
        }
      }

      handleOutput(record);
    } catch (Exception exception) {
      handleError(record, exception);
    }
  }

  /**
   * Closes the appender. Default implementation is a no-op; subclasses may override
   * to release resources.
   *
   * @throws LoggerException if closing fails
   */
  @Override
  public void close ()
    throws LoggerException {

  }
}
