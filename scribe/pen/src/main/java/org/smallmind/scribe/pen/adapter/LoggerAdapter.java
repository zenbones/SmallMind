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
package org.smallmind.scribe.pen.adapter;

import java.util.function.Supplier;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;

/**
 * Backend adapter interface that bridges the Scribe logging API to a concrete logging implementation,
 * exposing unified control over the logger name, parameter storage, context auto-fill, filters, appenders,
 * enhancers, level threshold, and three overloads for publishing log records.
 */
public interface LoggerAdapter {

  /**
   * Returns the name that identifies this logger within the backend.
   *
   * @return the logger name
   */
  String getName ();

  /**
   * Returns the {@link ParameterAdapter} that manages MDC-like contextual key/value pairs for this logger.
   *
   * @return the parameter adapter associated with this logger
   */
  ParameterAdapter getParameterAdapter ();

  /**
   * Returns whether this logger automatically captures caller context (class, method, line) for each record.
   *
   * @return {@code true} if logger context is filled automatically; {@code false} otherwise
   */
  boolean getAutoFillLoggerContext ();

  /**
   * Enables or disables automatic population of the logger context for each emitted record.
   *
   * @param autoFillLoggerContext {@code true} to capture caller context automatically; {@code false} to skip it
   */
  void setAutoFillLoggerContext (boolean autoFillLoggerContext);

  /**
   * Appends a filter to the end of this logger's filter chain; filters are evaluated in registration order
   * and any filter that rejects a record prevents it from reaching appenders.
   *
   * @param filter the filter to add
   */
  void addFilter (Filter filter);

  /**
   * Removes all filters currently registered with this logger.
   */
  void clearFilters ();

  /**
   * Registers an appender that will receive records that pass all filters.
   *
   * @param appender the appender to add
   */
  void addAppender (Appender appender);

  /**
   * Removes all appenders currently registered with this logger.
   */
  void clearAppenders ();

  /**
   * Registers an enhancer that is invoked on each record before it is dispatched to appenders,
   * allowing additional fields to be attached.
   *
   * @param enhancer the enhancer to add
   */
  void addEnhancer (Enhancer enhancer);

  /**
   * Removes all enhancers currently registered with this logger.
   */
  void clearEnhancers ();

  /**
   * Returns the minimum {@link Level} at which this logger will process records; records below this
   * threshold are discarded without further evaluation.
   *
   * @return the current level threshold
   */
  Level getLevel ();

  /**
   * Sets the minimum {@link Level} at which this logger will process records.
   *
   * @param level the new level threshold
   */
  void setLevel (Level level);

  /**
   * Constructs and publishes a log record from a printf-style format string and its arguments.
   *
   * @param level     the severity level for this record
   * @param throwable an optional throwable to attach to the record, or {@code null}
   * @param message   the message template, where {@code %s}-style placeholders are filled from {@code args}
   * @param args      arguments substituted into the message template
   */
  void logMessage (Level level, Throwable throwable, String message, Object... args);

  /**
   * Constructs and publishes a log record whose message is derived from the string representation
   * of the supplied object.
   *
   * @param level     the severity level for this record
   * @param throwable an optional throwable to attach to the record, or {@code null}
   * @param object    the object whose {@code toString()} value becomes the log message
   */
  void logMessage (Level level, Throwable throwable, Object object);

  /**
   * Constructs and publishes a log record whose message is obtained by invoking the supplier only if the
   * record will actually be emitted, avoiding unnecessary string construction.
   *
   * @param level     the severity level for this record
   * @param throwable an optional throwable to attach to the record, or {@code null}
   * @param supplier  a supplier whose return value becomes the log message; evaluated lazily
   */
  void logMessage (Level level, Throwable throwable, Supplier<String> supplier);
}
