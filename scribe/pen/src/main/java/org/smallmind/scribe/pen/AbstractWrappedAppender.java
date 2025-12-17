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
 * Base decorator for appenders that delegates all operations to an internal appender.
 * Subclasses can augment publishing by invoking {@link #publishToWrappedAppender(Record)}.
 */
public abstract class AbstractWrappedAppender implements Appender {

  private final Appender internalAppender;

  /**
   * Wraps the provided appender.
   *
   * @param internalAppender appender to delegate to
   */
  public AbstractWrappedAppender (Appender internalAppender) {

    this.internalAppender = internalAppender;
  }

  /**
   * Returns the name of the wrapped appender.
   *
   * @return configured appender name, or {@code null} if none
   */
  @Override
  public String getName () {

    return internalAppender.getName();
  }

  /**
   * Sets the name on the wrapped appender.
   *
   * @param name new appender name, may be {@code null}
   */
  @Override
  public void setName (String name) {

    internalAppender.setName(name);
  }

  /**
   * Clears all filters on the wrapped appender.
   */
  @Override
  public void clearFilters () {

    internalAppender.clearFilters();
  }

  /**
   * Replaces any existing filters with the supplied filter on the wrapped appender.
   *
   * @param filter filter that must approve records before output
   */
  @Override
  public synchronized void setFilter (Filter filter) {

    internalAppender.setFilter(filter);
  }

  /**
   * Adds an additional filter to the wrapped appender.
   *
   * @param filter filter to append to the evaluation chain
   */
  @Override
  public void addFilter (Filter filter) {

    internalAppender.addFilter(filter);
  }

  /**
   * Retrieves filters currently configured on the wrapped appender.
   *
   * @return array of filters in evaluation order
   */
  @Override
  public Filter[] getFilters () {

    return internalAppender.getFilters();
  }

  /**
   * Replaces filters on the wrapped appender with the given list.
   *
   * @param filterList filters to evaluate in order
   */
  @Override
  public void setFilters (List<Filter> filterList) {

    internalAppender.setFilters(filterList);
  }

  /**
   * Returns the error handler configured on the wrapped appender.
   *
   * @return error handler, or {@code null} if none
   */
  @Override
  public ErrorHandler getErrorHandler () {

    return internalAppender.getErrorHandler();
  }

  /**
   * Sets the error handler on the wrapped appender.
   *
   * @param errorHandler handler to receive failures
   */
  @Override
  public void setErrorHandler (ErrorHandler errorHandler) {

    internalAppender.setErrorHandler(errorHandler);
  }

  /**
   * Indicates whether the wrapped appender is currently active.
   *
   * @return {@code true} if active, otherwise {@code false}
   */
  @Override
  public boolean isActive () {

    return internalAppender.isActive();
  }

  /**
   * Enables or disables the wrapped appender.
   *
   * @param active {@code true} to allow publishing, {@code false} to ignore records
   */
  @Override
  public void setActive (boolean active) {

    internalAppender.setActive(active);
  }

  /**
   * Closes the wrapped appender.
   *
   * @throws InterruptedException if interrupted while closing
   * @throws LoggerException      if closing fails
   */
  @Override
  public void close ()
    throws InterruptedException, LoggerException {

    internalAppender.close();
  }

  /**
   * Publishes a record to the wrapped appender.
   *
   * @param record record to forward
   * @throws RuntimeException if the wrapped appender throws an unchecked exception
   */
  public void publishToWrappedAppender (Record<?> record) {

    internalAppender.publish(record);
  }
}
