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
 * Template that matches all loggers with a baseline priority.
 */
public class DefaultTemplate extends Template {

  /**
   * Creates a template with default level and context settings.
   */
  public DefaultTemplate () {

    super();
  }

  /**
   * Creates a template with the given level and context behavior.
   *
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   */
  public DefaultTemplate (Level level, boolean autoFillLoggerContext) {

    super(level, autoFillLoggerContext);
  }

  /**
   * Creates a template with level, context behavior, and appenders.
   *
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param appenders             appenders to attach
   */
  public DefaultTemplate (Level level, boolean autoFillLoggerContext, Appender... appenders) {

    super(level, autoFillLoggerContext, appenders);
  }

  /**
   * Creates a template with filters, appenders, enhancers, level, and context behavior.
   *
   * @param filters               filters to apply
   * @param appenders             appenders to attach
   * @param enhancers             enhancers to apply
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   */
  public DefaultTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext) {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);
  }

  /**
   * Matches any logger name with a minimal priority.
   *
   * @param loggerName logger name
   * @return {@code NO_MATCH + 1} for all loggers
   */
  @Override
  public int matchLogger (String loggerName) {

    return NO_MATCH + 1;
  }
}
