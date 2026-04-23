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
 * Decorator base class that forwards every {@link Appender} operation to a contained delegate
 * appender, giving subclasses a hook to intercept or augment publishing via
 * {@link #publishToWrappedAppender(Record)}.
 */
public abstract class AbstractWrappedAppender implements Appender {

  private final Appender internalAppender;

  /**
   * Constructs a wrapper around the given appender, delegating all operations to it.
   *
   * @param internalAppender the appender to wrap; must not be {@code null}
   */
  public AbstractWrappedAppender (Appender internalAppender) {

    this.internalAppender = internalAppender;
  }

  /**
   * Returns the name of the wrapped appender.
   *
   * @return appender name, or {@code null} if none has been set on the delegate
   */
  @Override
  public String getName () {

    return internalAppender.getName();
  }

  /**
   * Sets the name on the wrapped appender.
   *
   * @param name new appender name; may be {@code null}
   */
  @Override
  public void setName (String name) {

    internalAppender.setName(name);
  }

  /**
   * Removes all filters from the wrapped appender's filter chain.
   */
  @Override
  public void clearFilters () {

    internalAppender.clearFilters();
  }

  /**
   * Replaces the wrapped appender's entire filter chain with the single supplied filter.
   *
   * @param filter the sole filter that records must satisfy; must not be {@code null}
   */
  @Override
  public synchronized void setFilter (Filter filter) {

    internalAppender.setFilter(filter);
  }

  /**
   * Appends a filter to the wrapped appender's filter chain.
   *
   * @param filter additional filter to add to the evaluation chain
   */
  @Override
  public void addFilter (Filter filter) {

    internalAppender.addFilter(filter);
  }

  /**
   * Returns a snapshot of the filters installed on the wrapped appender.
   *
   * @return array of filters in their evaluation order; never {@code null}
   */
  @Override
  public Filter[] getFilters () {

    return internalAppender.getFilters();
  }

  /**
   * Replaces the wrapped appender's filter chain with the supplied list.
   *
   * @param filterList ordered list of filters to install; must not be {@code null}
   */
  @Override
  public void setFilters (List<Filter> filterList) {

    internalAppender.setFilters(filterList);
  }

  /**
   * Returns the error handler configured on the wrapped appender.
   *
   * @return the delegate's error handler, or {@code null} if none is set
   */
  @Override
  public ErrorHandler getErrorHandler () {

    return internalAppender.getErrorHandler();
  }

  /**
   * Sets the error handler on the wrapped appender.
   *
   * @param errorHandler handler to receive output failures from the delegate; may be {@code null}
   */
  @Override
  public void setErrorHandler (ErrorHandler errorHandler) {

    internalAppender.setErrorHandler(errorHandler);
  }

  /**
   * Indicates whether the wrapped appender is currently enabled and accepting records.
   *
   * @return {@code true} if the delegate is active; {@code false} if it is disabled
   */
  @Override
  public boolean isActive () {

    return internalAppender.isActive();
  }

  /**
   * Enables or disables the wrapped appender.
   *
   * @param active {@code true} to enable publishing on the delegate; {@code false} to disable it
   */
  @Override
  public void setActive (boolean active) {

    internalAppender.setActive(active);
  }

  /**
   * Closes the wrapped appender and releases its resources.
   *
   * @throws InterruptedException if the closing operation is interrupted
   * @throws LoggerException      if an error occurs while closing the delegate
   */
  @Override
  public void close ()
    throws InterruptedException, LoggerException {

    internalAppender.close();
  }

  /**
   * Forwards the record directly to the wrapped appender's {@link Appender#publish(Record)} method,
   * bypassing any overrides in this decorator.
   *
   * @param record the log record to forward to the delegate
   */
  public void publishToWrappedAppender (Record<?> record) {

    internalAppender.publish(record);
  }
}
