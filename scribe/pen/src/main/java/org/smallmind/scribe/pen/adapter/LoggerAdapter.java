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
 * Abstraction over backend-specific loggers, exposing common operations for scribe.
 */
public interface LoggerAdapter {

  /**
   * Returns the logger name.
   *
   * @return logger name
   */
  String getName ();

  /**
   * Returns the parameter adapter used for MDC-like values.
   *
   * @return parameter adapter
   */
  ParameterAdapter getParameterAdapter ();

  /**
   * Indicates whether logger context should be auto-populated.
   *
   * @return {@code true} when context auto-fill is enabled
   */
  boolean getAutoFillLoggerContext ();

  /**
   * Enables or disables automatic context population.
   *
   * @param autoFillLoggerContext {@code true} to capture context automatically
   */
  void setAutoFillLoggerContext (boolean autoFillLoggerContext);

  /**
   * Registers a filter that can veto log records.
   *
   * @param filter filter to add
   */
  void addFilter (Filter filter);

  /**
   * Removes all configured filters.
   */
  void clearFilters ();

  /**
   * Registers an appender that will receive records.
   *
   * @param appender appender to add
   */
  void addAppender (Appender appender);

  /**
   * Removes all configured appenders.
   */
  void clearAppenders ();

  /**
   * Registers an enhancer invoked before publishing.
   *
   * @param enhancer enhancer to add
   */
  void addEnhancer (Enhancer enhancer);

  /**
   * Removes all configured enhancers.
   */
  void clearEnhancers ();

  /**
   * Returns the current logging level threshold.
   *
   * @return level
   */
  Level getLevel ();

  /**
   * Sets the logging level threshold.
   *
   * @param level level to set
   */
  void setLevel (Level level);

  /**
   * Logs a formatted message.
   *
   * @param level     level to log at
   * @param throwable optional throwable
   * @param message   message template
   * @param args      message arguments
   */
  void logMessage (Level level, Throwable throwable, String message, Object... args);

  /**
   * Logs an arbitrary object's string representation.
   *
   * @param level     level to log at
   * @param throwable optional throwable
   * @param object    object to log
   */
  void logMessage (Level level, Throwable throwable, Object object);

  /**
   * Logs a lazily supplied message.
   *
   * @param level     level to log at
   * @param throwable optional throwable
   * @param supplier  supplier that produces the message
   */
  void logMessage (Level level, Throwable throwable, Supplier<String> supplier);
}
