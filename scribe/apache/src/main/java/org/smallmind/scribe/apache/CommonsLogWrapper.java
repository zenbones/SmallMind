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
 * Bridges Apache Commons Logging to the SmallMind scribe logger implementation.
 * This wrapper delegates all log level checks and log statements to a {@link Logger}
 * obtained from {@link LoggerManager}, allowing Commons Logging clients to emit log
 * events through the scribe pipeline.
 */
public class CommonsLogWrapper implements Log {

  private final String name;

  static {

    LoggerManager.addLoggingPackagePrefix("org.apache.commons.logging.");
  }

  /**
   * Creates a wrapper that delegates Commons Logging operations for the given logger name.
   *
   * @param name the logger name to resolve via {@link LoggerManager}
   */
  public CommonsLogWrapper (String name) {

    this.name = name;
  }

  /**
   * Resolves the scribe {@link Logger} instance for this wrapper.
   *
   * @return the logger associated with the configured name
   */
  private Logger getLogger () {

    return LoggerManager.getLogger(name);
  }

  /**
   * Indicates whether DEBUG level logging is enabled.
   *
   * @return {@code true} if debug events should be emitted
   */
  public boolean isDebugEnabled () {

    return getLogger().getLevel().noGreater(Level.DEBUG);
  }

  /**
   * Indicates whether ERROR level logging is enabled.
   *
   * @return {@code true} if error events should be emitted
   */
  public boolean isErrorEnabled () {

    return getLogger().getLevel().noGreater(Level.ERROR);
  }

  /**
   * Indicates whether FATAL level logging is enabled.
   *
   * @return {@code true} if fatal events should be emitted
   */
  public boolean isFatalEnabled () {

    return getLogger().getLevel().noGreater(Level.FATAL);
  }

  /**
   * Indicates whether INFO level logging is enabled.
   *
   * @return {@code true} if info events should be emitted
   */
  public boolean isInfoEnabled () {

    return getLogger().getLevel().noGreater(Level.INFO);
  }

  /**
   * Indicates whether TRACE level logging is enabled.
   *
   * @return {@code true} if trace events should be emitted
   */
  public boolean isTraceEnabled () {

    return getLogger().getLevel().noGreater(Level.TRACE);
  }

  /**
   * Indicates whether WARN level logging is enabled.
   *
   * @return {@code true} if warn events should be emitted
   */
  public boolean isWarnEnabled () {

    return getLogger().getLevel().noGreater(Level.WARN);
  }

  /**
   * Logs a TRACE level message.
   *
   * @param o message payload to log
   */
  public void trace (Object o) {

    getLogger().trace(o);
  }

  /**
   * Logs a TRACE level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void trace (Object o, Throwable throwable) {

    getLogger().trace(throwable, o);
  }

  /**
   * Logs a DEBUG level message.
   *
   * @param o message payload to log
   */
  public void debug (Object o) {

    getLogger().debug(o);
  }

  /**
   * Logs a DEBUG level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void debug (Object o, Throwable throwable) {

    getLogger().debug(throwable, o);
  }

  /**
   * Logs an INFO level message.
   *
   * @param o message payload to log
   */
  public void info (Object o) {

    getLogger().info(o);
  }

  /**
   * Logs an INFO level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void info (Object o, Throwable throwable) {

    getLogger().info(throwable, o);
  }

  /**
   * Logs a WARN level message.
   *
   * @param o message payload to log
   */
  public void warn (Object o) {

    getLogger().warn(o);
  }

  /**
   * Logs a WARN level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void warn (Object o, Throwable throwable) {

    getLogger().warn(throwable, o);
  }

  /**
   * Logs an ERROR level message.
   *
   * @param o message payload to log
   */
  public void error (Object o) {

    getLogger().error(o);
  }

  /**
   * Logs an ERROR level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void error (Object o, Throwable throwable) {

    getLogger().error(throwable, o);
  }

  /**
   * Logs a FATAL level message.
   *
   * @param o message payload to log
   */
  public void fatal (Object o) {

    getLogger().fatal(o);
  }

  /**
   * Logs a FATAL level message with an associated failure.
   *
   * @param o         message payload to log
   * @param throwable exception or error to attach
   */
  public void fatal (Object o, Throwable throwable) {

    getLogger().fatal(throwable, o);
  }
}
