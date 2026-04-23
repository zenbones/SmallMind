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

import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Template} that matches exactly one logger by name with the highest possible priority
 * ({@code Integer.MAX_VALUE}), ensuring it overrides any less-specific template for that logger.
 * An {@link java.util.concurrent.atomic.AtomicReference} guards single-initialization of the target name.
 */
public class PersonalizedTemplate extends Template {

  private final AtomicReference<String> loggerNameRef = new AtomicReference<String>();

  /**
   * Constructs an uninitialized template; {@link #setLoggerName(String)} must be called exactly
   * once before this template is used.
   */
  public PersonalizedTemplate () {

    super();
  }

  /**
   * Constructs a template bound to the given logger name.
   *
   * @param loggerName the exact logger name this template will match
   */
  public PersonalizedTemplate (String loggerName) {

    super();

    loggerNameRef.set(loggerName);
  }

  /**
   * Constructs a template with an explicit log level, context-fill behavior, and bound logger name.
   *
   * @param level                 the default {@link Level} for the matched logger
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param loggerName            the exact logger name this template will match
   * @throws LoggerException if internal initialization fails
   */
  public PersonalizedTemplate (Level level, boolean autoFillLoggerContext, String loggerName)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    loggerNameRef.set(loggerName);
  }

  /**
   * Constructs a fully specified template with filters, appenders, enhancers, level, context behavior,
   * and the bound logger name.
   *
   * @param filters               filters applied before a record is forwarded to appenders
   * @param appenders             appenders that receive matching records
   * @param enhancers             enhancers that decorate records before they are appended
   * @param level                 the default {@link Level} for the matched logger
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param loggerName            the exact logger name this template will match
   * @throws LoggerException if internal initialization fails
   */
  public PersonalizedTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String loggerName)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    loggerNameRef.set(loggerName);
  }

  /**
   * Binds this template to the given logger name; may only be called once.
   *
   * @param loggerName the exact logger name to match
   * @throws LoggerRuntimeException if this template has already been bound to a logger name
   */
  public void setLoggerName (String loggerName) {

    if (!loggerNameRef.compareAndSet(null, loggerName)) {
      throw new LoggerRuntimeException("PersonalizedTemplate has been previously initialized with a logger name");
    }
  }

  /**
   * Returns {@code Integer.MAX_VALUE} when {@code loggerName} exactly equals the configured name,
   * giving this template the highest possible priority, or {@link Template#NO_MATCH} for any other name.
   *
   * @param loggerName the logger name to evaluate
   * @return {@code Integer.MAX_VALUE} on an exact match; {@link Template#NO_MATCH} otherwise
   * @throws LoggerRuntimeException if this template was never initialized with a logger name
   */
  public int matchLogger (String loggerName) {

    if (loggerNameRef.get() == null) {
      throw new LoggerRuntimeException("PersonalizedTemplate was never initialized with a logger name");
    }

    return loggerNameRef.get().equals(loggerName) ? Integer.MAX_VALUE : NO_MATCH;
  }
}
