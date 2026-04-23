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
 * Skeletal {@link Appender} implementation that manages a filter chain, an active flag, and an
 * {@link ErrorHandler}, routing each published {@link Record} through the filter chain before
 * delegating to the concrete {@link #handleOutput(Record)} method.
 */
public abstract class AbstractAppender implements Appender {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private ErrorHandler errorHandler;
  private String name;
  private boolean active = true;

  /**
   * Constructs an unnamed appender with no error handler and an empty filter chain.
   */
  public AbstractAppender () {

    this(null, null);
  }

  /**
   * Constructs an unnamed appender with the given error handler and an empty filter chain.
   *
   * @param errorHandler handler invoked when {@link #handleOutput(Record)} throws; may be {@code null}
   */
  public AbstractAppender (ErrorHandler errorHandler) {

    this(null, errorHandler);
  }

  /**
   * Constructs a named appender with the given error handler and an empty filter chain.
   *
   * @param name         name used to identify this appender; may be {@code null}
   * @param errorHandler handler invoked when {@link #handleOutput(Record)} throws; may be {@code null}
   */
  public AbstractAppender (String name, ErrorHandler errorHandler) {

    this.name = name;
    this.errorHandler = errorHandler;

    filterList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Writes the record to the concrete output target after the filter chain has approved it.
   *
   * @param record the log record to emit
   * @throws Exception if an I/O or encoding error occurs during output
   */
  public abstract void handleOutput (Record<?> record)
    throws Exception;

  /**
   * Returns the name assigned to this appender.
   *
   * @return appender name, or {@code null} if no name has been set
   */
  @Override
  public String getName () {

    return name;
  }

  /**
   * Sets the name used to identify this appender in configuration and diagnostics.
   *
   * @param name new appender name; may be {@code null}
   */
  @Override
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Removes all filters from the filter chain so that every record is passed to output unconditionally.
   */
  @Override
  public synchronized void clearFilters () {

    filterList.clear();
  }

  /**
   * Replaces the entire filter chain with a single filter.
   *
   * @param filter the sole filter that records must satisfy before being output
   */
  @Override
  public synchronized void setFilter (Filter filter) {

    filterList.clear();
    filterList.add(filter);
  }

  /**
   * Appends a filter to the end of the filter chain.
   *
   * @param filter additional filter that records must satisfy before being output
   */
  @Override
  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);
  }

  /**
   * Returns a snapshot of the current filter chain.
   *
   * @return array of filters in their evaluation order; never {@code null}
   */
  @Override
  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  /**
   * Replaces the entire filter chain with the supplied list of filters.
   *
   * @param replacementFilterList ordered list of filters to install; must not be {@code null}
   */
  @Override
  public synchronized void setFilters (List<Filter> replacementFilterList) {

    filterList.clear();
    filterList.addAll(replacementFilterList);
  }

  /**
   * Returns the error handler that receives exceptions thrown by {@link #handleOutput(Record)}.
   *
   * @return the configured error handler, or {@code null} if none has been set
   */
  @Override
  public ErrorHandler getErrorHandler () {

    return errorHandler;
  }

  /**
   * Sets the error handler invoked when {@link #handleOutput(Record)} throws an exception.
   *
   * @param errorHandler handler to receive output failures; may be {@code null}
   */
  @Override
  public void setErrorHandler (ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  /**
   * Indicates whether this appender is currently accepting and processing records.
   *
   * @return {@code true} if publishing is enabled; {@code false} if the appender is disabled
   */
  @Override
  public boolean isActive () {

    return active;
  }

  /**
   * Enables or disables publishing on this appender.
   *
   * @param active {@code true} to enable publishing; {@code false} to silently drop all records
   */
  @Override
  public void setActive (boolean active) {

    this.active = active;
  }

  /**
   * Runs the record through the filter chain and, if every filter approves it, delegates to
   * {@link #handleOutput(Record)}; any exception thrown by output is forwarded to the configured
   * error handler.
   *
   * @param record the log record to evaluate and potentially emit
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
   * Releases any resources held by this appender. The default implementation is a no-op;
   * subclasses should override this method to close streams, sockets, or other resources.
   *
   * @throws LoggerException if an error occurs while releasing resources
   */
  @Override
  public void close ()
    throws LoggerException {

  }
}
