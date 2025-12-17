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

import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

/**
 * Template that matches loggers by class-name pattern using dot-notation.
 */
public class ClassNameTemplate extends Template {

  private DotNotation notation;

  /**
   * Creates an empty template; configure with {@link #setPattern(String)} before use.
   */
  public ClassNameTemplate () {

    super();
  }

  /**
   * Creates a template with the supplied class-name pattern.
   *
   * @param pattern dot-notation pattern used to match logger names
   * @throws LoggerException if the pattern is invalid
   */
  public ClassNameTemplate (String pattern)
    throws LoggerException {

    super();

    setPattern(pattern);
  }

  /**
   * Creates a template with level, context behavior, and class-name pattern.
   *
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param pattern               dot-notation pattern used to match logger names
   * @throws LoggerException if the pattern is invalid
   */
  public ClassNameTemplate (Level level, boolean autoFillLoggerContext, String pattern)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    setPattern(pattern);
  }

  /**
   * Creates a template with level, context behavior, appenders, and class-name pattern.
   *
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param pattern               dot-notation pattern used to match logger names
   * @param appenders             appenders to attach when matched
   * @throws LoggerException if the pattern is invalid
   */
  public ClassNameTemplate (Level level, boolean autoFillLoggerContext, String pattern, Appender... appenders)
    throws LoggerException {

    super(level, autoFillLoggerContext, appenders);

    setPattern(pattern);
  }

  /**
   * Creates a template with filters, appenders, enhancers, level, context behavior, and class-name pattern.
   *
   * @param filters               filters to apply
   * @param appenders             appenders to attach
   * @param enhancers             enhancers to apply
   * @param level                 default log level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param pattern               dot-notation pattern used to match logger names
   * @throws LoggerException if the pattern is invalid
   */
  public ClassNameTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String pattern)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    setPattern(pattern);
  }

  /**
   * Sets the dot-notation pattern used for matching logger names.
   *
   * @param pattern pattern to evaluate against logger names
   * @throws LoggerException if the pattern cannot be parsed
   */
  public void setPattern (String pattern)
    throws LoggerException {

    try {
      notation = new DotNotation(pattern);
    } catch (DotNotationException dotNotationException) {
      throw new LoggerException(dotNotationException);
    }
  }

  /**
   * Computes the match score for the provided logger name.
   *
   * @param loggerName logger name to evaluate
   * @return match value or {@link Template#NO_MATCH} if not matched
   */
  @Override
  public int matchLogger (String loggerName) {

    return notation.calculateValue(loggerName, NO_MATCH);
  }
}
