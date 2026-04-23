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

import java.io.Serializable;
import java.util.function.Supplier;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprintFactory;

/**
 * Primary user-facing logging API for the Scribe framework, providing per-level convenience methods
 * (trace, debug, info, warn, error, fatal) and generic {@code log} overloads. All operations are
 * delegated to a {@link LoggerAdapter} obtained from the active
 * {@link org.smallmind.scribe.pen.adapter.LoggingBlueprint}.
 */
public class Logger {

  private final LoggerAdapter loggerAdapter;

  /**
   * Constructs a logger whose name is the canonical name of the supplied class.
   *
   * @param loggableClass class whose canonical name is used as the logger name; must not be {@code null}
   */
  public Logger (Class loggableClass) {

    this(loggableClass.getCanonicalName());
  }

  /**
   * Constructs a logger with the given name, obtaining its adapter from the active logging blueprint.
   *
   * @param name the logger name; must not be {@code null}
   */
  public Logger (String name) {

    loggerAdapter = LoggingBlueprintFactory.getLoggingBlueprint().getLoggingAdapter(name);
  }

  /**
   * Returns the conventional placeholder string used when a logger name cannot be determined.
   *
   * @return the literal string {@code "unknown"}
   */
  public static String unknown () {

    return "unknown";
  }

  /**
   * Returns the name that identifies this logger.
   *
   * @return logger name; never {@code null}
   */
  public String getName () {

    return loggerAdapter.getName();
  }

  /**
   * Returns the {@link Template} currently associated with this logger by {@link LoggerManager}.
   *
   * @return the active template, or {@code null} if no template has been matched to this logger
   */
  public Template getTemplate () {

    return LoggerManager.getTemplate(this);
  }

  /**
   * Stores or replaces a named contextual parameter that will be attached to subsequent log records.
   *
   * @param key   parameter name; must not be {@code null}
   * @param value serializable value to associate with the key; may be {@code null}
   */
  public void putParameter (String key, Serializable value) {

    loggerAdapter.getParameterAdapter().put(key, value);
  }

  /**
   * Removes the contextual parameter with the given key.
   *
   * @param key parameter name to remove; no-op if the key is not present
   */
  public void removeParameter (String key) {

    loggerAdapter.getParameterAdapter().remove(key);
  }

  /**
   * Removes all contextual parameters from this logger.
   */
  public void clearParameters () {

    loggerAdapter.getParameterAdapter().clear();
  }

  /**
   * Returns all contextual parameters currently stored on this logger.
   *
   * @return array of parameters; never {@code null} but may be empty
   */
  public Parameter[] getParameters () {

    return loggerAdapter.getParameterAdapter().getParameters();
  }

  /**
   * Indicates whether this logger automatically populates context data on each record.
   *
   * @return {@code true} if context auto-fill is enabled; {@code false} otherwise
   */
  public boolean getAutoFillLoggerContext () {

    return loggerAdapter.getAutoFillLoggerContext();
  }

  /**
   * Enables or disables automatic population of context data on each record produced by this logger.
   *
   * @param autoFillLoggerContext {@code true} to enable context auto-fill; {@code false} to disable it
   */
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    loggerAdapter.setAutoFillLoggerContext(autoFillLoggerContext);
  }

  /**
   * Adds multiple filters to this logger's filter chain in array order.
   *
   * @param filters filters to add; must not be {@code null}
   */
  public void addFilters (Filter[] filters) {

    for (Filter filter : filters) {
      addFilter(filter);
    }
  }

  /**
   * Appends a single filter to this logger's filter chain.
   *
   * @param filter filter to add; must not be {@code null}
   */
  public void addFilter (Filter filter) {

    loggerAdapter.addFilter(filter);
  }

  /**
   * Removes all filters from this logger's filter chain.
   */
  public void clearFilters () {

    loggerAdapter.clearFilters();
  }

  /**
   * Registers multiple appenders on this logger in array order.
   *
   * @param appenders appenders to register; must not be {@code null}
   */
  public void addAppenders (Appender[] appenders) {

    for (Appender appender : appenders) {
      addAppender(appender);
    }
  }

  /**
   * Registers a single appender on this logger.
   *
   * @param appender appender to register; must not be {@code null}
   */
  public void addAppender (Appender appender) {

    loggerAdapter.addAppender(appender);
  }

  /**
   * Removes all appenders from this logger.
   */
  public void clearAppenders () {

    loggerAdapter.clearAppenders();
  }

  /**
   * Adds an enhancer that may mutate each record before it is published to appenders.
   *
   * @param enhancer enhancer to add; must not be {@code null}
   */
  public void addEnhancer (Enhancer enhancer) {

    loggerAdapter.addEnhancer(enhancer);
  }

  /**
   * Removes all enhancers from this logger.
   */
  public void clearEnhancers () {

    loggerAdapter.clearEnhancers();
  }

  /**
   * Returns the level threshold currently set on this logger.
   *
   * @return active level; never {@code null}
   */
  public Level getLevel () {

    return loggerAdapter.getLevel();
  }

  /**
   * Sets the level threshold below which records are not published.
   *
   * @param level new threshold level; must not be {@code null}
   * @throws IllegalArgumentException if {@code level} is {@code null}
   */
  public void setLevel (Level level) {

    if (level == null) {
      throw new IllegalArgumentException("Can't set a 'null' default level");
    }

    loggerAdapter.setLevel(level);
  }

  /**
   * Publishes a record at {@link Level#TRACE} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void trace (Throwable throwable) {

    log(Level.TRACE, throwable);
  }

  /**
   * Publishes a record at {@link Level#TRACE} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void trace (String message, Object... args) {

    log(Level.TRACE, message, args);
  }

  /**
   * Publishes a record at {@link Level#TRACE} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void trace (Throwable throwable, String message, Object... args) {

    log(Level.TRACE, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#TRACE} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void trace (Object object) {

    log(Level.TRACE, object);
  }

  /**
   * Publishes a record at {@link Level#TRACE} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void trace (Supplier<String> supplier) {

    log(Level.TRACE, supplier);
  }

  /**
   * Publishes a record at {@link Level#TRACE} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void trace (Throwable throwable, Object object) {

    log(Level.TRACE, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#TRACE} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void trace (Throwable throwable, Supplier<String> supplier) {

    log(Level.TRACE, throwable, supplier);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void debug (Throwable throwable) {

    log(Level.DEBUG, throwable);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void debug (String message, Object... args) {

    log(Level.DEBUG, message, args);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void debug (Throwable throwable, String message, Object... args) {

    log(Level.DEBUG, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void debug (Object object) {

    log(Level.DEBUG, object);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void debug (Supplier<String> supplier) {

    log(Level.DEBUG, supplier);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void debug (Throwable throwable, Object object) {

    log(Level.DEBUG, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#DEBUG} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void debug (Throwable throwable, Supplier<String> supplier) {

    log(Level.DEBUG, throwable, supplier);
  }

  /**
   * Publishes a record at {@link Level#INFO} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void info (Throwable throwable) {

    log(Level.INFO, throwable);
  }

  /**
   * Publishes a record at {@link Level#INFO} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void info (String message, Object... args) {

    log(Level.INFO, message, args);
  }

  /**
   * Publishes a record at {@link Level#INFO} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void info (Throwable throwable, String message, Object... args) {

    log(Level.INFO, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#INFO} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void info (Object object) {

    log(Level.INFO, object);
  }

  /**
   * Publishes a record at {@link Level#INFO} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void info (Supplier<String> supplier) {

    log(Level.INFO, supplier);
  }

  /**
   * Publishes a record at {@link Level#INFO} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void info (Throwable throwable, Object object) {

    log(Level.INFO, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#INFO} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void info (Throwable throwable, Supplier<String> supplier) {

    log(Level.INFO, throwable, supplier);
  }

  /**
   * Publishes a record at {@link Level#WARN} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void warn (Throwable throwable) {

    log(Level.WARN, throwable);
  }

  /**
   * Publishes a record at {@link Level#WARN} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void warn (String message, Object... args) {

    log(Level.WARN, message, args);
  }

  /**
   * Publishes a record at {@link Level#WARN} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void warn (Throwable throwable, String message, Object... args) {

    log(Level.WARN, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#WARN} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void warn (Object object) {

    log(Level.WARN, object);
  }

  /**
   * Publishes a record at {@link Level#WARN} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void warn (Supplier<String> supplier) {

    log(Level.WARN, supplier);
  }

  /**
   * Publishes a record at {@link Level#WARN} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void warn (Throwable throwable, Object object) {

    log(Level.WARN, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#WARN} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void warn (Throwable throwable, Supplier<String> supplier) {

    log(Level.WARN, throwable, supplier);
  }

  /**
   * Publishes a record at {@link Level#ERROR} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void error (Throwable throwable) {

    log(Level.ERROR, throwable);
  }

  /**
   * Publishes a record at {@link Level#ERROR} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void error (String message, Object... args) {

    log(Level.ERROR, message, args);
  }

  /**
   * Publishes a record at {@link Level#ERROR} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void error (Throwable throwable, String message, Object... args) {

    log(Level.ERROR, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#ERROR} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void error (Object object) {

    log(Level.ERROR, object);
  }

  /**
   * Publishes a record at {@link Level#ERROR} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void error (Supplier<String> supplier) {

    log(Level.ERROR, supplier);
  }

  /**
   * Publishes a record at {@link Level#ERROR} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void error (Throwable throwable, Object object) {

    log(Level.ERROR, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#ERROR} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void error (Throwable throwable, Supplier<String> supplier) {

    log(Level.ERROR, throwable, supplier);
  }

  /**
   * Publishes a record at {@link Level#FATAL} carrying only the given throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void fatal (Throwable throwable) {

    log(Level.FATAL, throwable);
  }

  /**
   * Publishes a record at {@link Level#FATAL} with a formatted message and no throwable.
   *
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void fatal (String message, Object... args) {

    log(Level.FATAL, message, args);
  }

  /**
   * Publishes a record at {@link Level#FATAL} with both a formatted message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void fatal (Throwable throwable, String message, Object... args) {

    log(Level.FATAL, throwable, message, args);
  }

  /**
   * Publishes a record at {@link Level#FATAL} using the string representation of the given object.
   *
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void fatal (Object object) {

    log(Level.FATAL, object);
  }

  /**
   * Publishes a record at {@link Level#FATAL} with a lazily evaluated message, avoiding string
   * construction when the level is not enabled.
   *
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void fatal (Supplier<String> supplier) {

    log(Level.FATAL, supplier);
  }

  /**
   * Publishes a record at {@link Level#FATAL} carrying an object message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void fatal (Throwable throwable, Object object) {

    log(Level.FATAL, throwable, object);
  }

  /**
   * Publishes a record at {@link Level#FATAL} with a lazily evaluated message and a throwable.
   *
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void fatal (Throwable throwable, Supplier<String> supplier) {

    log(Level.FATAL, throwable, supplier);
  }

  /**
   * Publishes a record carrying only a throwable at the given level, falling back to this logger's
   * current level when {@code level} is {@code null}.
   *
   * @param level     level at which to publish; uses the current logger level when {@code null}
   * @param throwable throwable to attach to the record; must not be {@code null}
   */
  public void log (Level level, Throwable throwable) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, null);
  }

  /**
   * Publishes a record with a formatted message and no throwable at the given level, falling back to
   * this logger's current level when {@code level} is {@code null}.
   *
   * @param level   level at which to publish; uses the current logger level when {@code null}
   * @param message {@link String#format}-style message template; must not be {@code null}
   * @param args    arguments substituted into the template
   */
  public void log (Level level, String message, Object... args) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, message, args);
  }

  /**
   * Publishes a record with a formatted message and a throwable at the given level, falling back to
   * this logger's current level when {@code level} is {@code null}.
   *
   * @param level     level at which to publish; uses the current logger level when {@code null}
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param message   {@link String#format}-style message template; must not be {@code null}
   * @param args      arguments substituted into the template
   */
  public void log (Level level, Throwable throwable, String message, Object... args) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, message, args);
  }

  /**
   * Publishes a record using the string representation of an object at the given level, falling back
   * to this logger's current level when {@code level} is {@code null}.
   *
   * @param level  level at which to publish; uses the current logger level when {@code null}
   * @param object object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void log (Level level, Object object) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, object);
  }

  /**
   * Publishes a record with a lazily evaluated message at the given level, falling back to this
   * logger's current level when {@code level} is {@code null}.
   *
   * @param level    level at which to publish; uses the current logger level when {@code null}
   * @param supplier supplier that produces the log message; must not be {@code null}
   */
  public void log (Level level, Supplier<String> supplier) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, supplier);
  }

  /**
   * Publishes a record carrying an object message and a throwable at the given level, falling back
   * to this logger's current level when {@code level} is {@code null}.
   *
   * @param level     level at which to publish; uses the current logger level when {@code null}
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param object    object whose {@code toString()} value becomes the log message; may be {@code null}
   */
  public void log (Level level, Throwable throwable, Object object) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, object);
  }

  /**
   * Publishes a record with a lazily evaluated message and a throwable at the given level, falling
   * back to this logger's current level when {@code level} is {@code null}.
   *
   * @param level     level at which to publish; uses the current logger level when {@code null}
   * @param throwable throwable to attach to the record; must not be {@code null}
   * @param supplier  supplier that produces the log message; must not be {@code null}
   */
  public void log (Level level, Throwable throwable, Supplier<String> supplier) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, supplier);
  }
}
