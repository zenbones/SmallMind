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
package org.smallmind.scribe.apache;

import org.apache.commons.logging.Log;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Implements the Apache Commons Logging {@link Log} interface by delegating all calls to a
 * scribe {@link Logger}, allowing libraries that depend on Commons Logging to emit events
 * through the scribe pipeline without any additional configuration.
 * Registers {@code "org.apache.commons.logging."} as a logging package prefix so that
 * call-site context is resolved correctly past the Commons Logging frames.
 */
public class CommonsLogWrapper implements Log {

  private final String name;

  static {

    LoggerManager.addLoggingPackagePrefix("org.apache.commons.logging.");
  }

  /**
   * Constructs a wrapper bound to the given logger name.
   * The underlying scribe {@link Logger} is resolved lazily on each log call via
   * {@link LoggerManager#getLogger(String)}.
   *
   * @param name the logger name passed to {@link LoggerManager} on every delegation
   */
  public CommonsLogWrapper (String name) {

    this.name = name;
  }

  /**
   * Looks up the scribe logger for this wrapper's name on every call so that template
   * re-association is always reflected without caching a stale reference.
   *
   * @return the current {@link Logger} for {@link #name}
   */
  private Logger getLogger () {

    return LoggerManager.getLogger(name);
  }

  /**
   * Reports whether DEBUG-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is DEBUG or finer
   */
  public boolean isDebugEnabled () {

    return getLogger().getLevel().noGreater(Level.DEBUG);
  }

  /**
   * Reports whether ERROR-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is ERROR or finer
   */
  public boolean isErrorEnabled () {

    return getLogger().getLevel().noGreater(Level.ERROR);
  }

  /**
   * Reports whether FATAL-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is FATAL or finer
   */
  public boolean isFatalEnabled () {

    return getLogger().getLevel().noGreater(Level.FATAL);
  }

  /**
   * Reports whether INFO-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is INFO or finer
   */
  public boolean isInfoEnabled () {

    return getLogger().getLevel().noGreater(Level.INFO);
  }

  /**
   * Reports whether TRACE-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is TRACE or finer
   */
  public boolean isTraceEnabled () {

    return getLogger().getLevel().noGreater(Level.TRACE);
  }

  /**
   * Reports whether WARN-level events will pass the effective level threshold.
   *
   * @return {@code true} if the effective level is WARN or finer
   */
  public boolean isWarnEnabled () {

    return getLogger().getLevel().noGreater(Level.WARN);
  }

  /**
   * Emits a TRACE-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void trace (Object o) {

    getLogger().trace(o);
  }

  /**
   * Emits a TRACE-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void trace (Object o, Throwable throwable) {

    getLogger().trace(throwable, o);
  }

  /**
   * Emits a DEBUG-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void debug (Object o) {

    getLogger().debug(o);
  }

  /**
   * Emits a DEBUG-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void debug (Object o, Throwable throwable) {

    getLogger().debug(throwable, o);
  }

  /**
   * Emits an INFO-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void info (Object o) {

    getLogger().info(o);
  }

  /**
   * Emits an INFO-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void info (Object o, Throwable throwable) {

    getLogger().info(throwable, o);
  }

  /**
   * Emits a WARN-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void warn (Object o) {

    getLogger().warn(o);
  }

  /**
   * Emits a WARN-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void warn (Object o, Throwable throwable) {

    getLogger().warn(throwable, o);
  }

  /**
   * Emits an ERROR-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void error (Object o) {

    getLogger().error(o);
  }

  /**
   * Emits an ERROR-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void error (Object o, Throwable throwable) {

    getLogger().error(throwable, o);
  }

  /**
   * Emits a FATAL-level event with the given message object.
   *
   * @param o object whose {@code toString()} provides the log message
   */
  public void fatal (Object o) {

    getLogger().fatal(o);
  }

  /**
   * Emits a FATAL-level event with the given message object and attached throwable.
   *
   * @param o         object whose {@code toString()} provides the log message
   * @param throwable exception or error to attach to the event
   */
  public void fatal (Object o, Throwable throwable) {

    getLogger().fatal(throwable, o);
  }
}
