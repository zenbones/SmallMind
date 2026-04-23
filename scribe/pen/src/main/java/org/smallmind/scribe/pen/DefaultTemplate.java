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

/**
 * A catch-all {@link Template} that matches every logger name with the lowest possible non-zero
 * priority ({@code NO_MATCH + 1}), making it the fallback when no more-specific template applies.
 */
public class DefaultTemplate extends Template {

  /**
   * Constructs a default template using inherited default level and context settings.
   */
  public DefaultTemplate () {

    super();
  }

  /**
   * Constructs a default template with an explicit log level and context-fill behavior.
   *
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   */
  public DefaultTemplate (Level level, boolean autoFillLoggerContext) {

    super(level, autoFillLoggerContext);
  }

  /**
   * Constructs a default template with a log level, context-fill behavior, and one or more appenders.
   *
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param appenders             the appenders to attach to every logger matched by this template
   */
  public DefaultTemplate (Level level, boolean autoFillLoggerContext, Appender... appenders) {

    super(level, autoFillLoggerContext, appenders);
  }

  /**
   * Constructs a fully specified default template with filters, appenders, enhancers, level, and context behavior.
   *
   * @param filters               filters applied before a record is forwarded to appenders
   * @param appenders             appenders that receive matching records
   * @param enhancers             enhancers that decorate records before they are appended
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   */
  public DefaultTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext) {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);
  }

  /**
   * Returns {@code NO_MATCH + 1} for every logger name, giving this template the lowest priority
   * among templates that produce a positive match score.
   *
   * @param loggerName the name of the logger being matched
   * @return {@code NO_MATCH + 1} unconditionally
   */
  @Override
  public int matchLogger (String loggerName) {

    return NO_MATCH + 1;
  }
}
