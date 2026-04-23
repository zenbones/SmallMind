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
 * A {@link Template} that selects loggers by matching their names against a dot-notation pattern,
 * delegating the match score calculation to {@link org.smallmind.nutsnbolts.util.DotNotation#calculateValue}.
 */
public class ClassNameTemplate extends Template {

  private DotNotation notation;

  /**
   * Constructs an unconfigured template; {@link #setPattern(String)} must be called before this
   * template is registered.
   */
  public ClassNameTemplate () {

    super();
  }

  /**
   * Constructs a template that matches logger names against the given dot-notation pattern.
   *
   * @param pattern the dot-notation pattern used to match logger names
   * @throws LoggerException if {@code pattern} cannot be parsed
   */
  public ClassNameTemplate (String pattern)
    throws LoggerException {

    super();

    setPattern(pattern);
  }

  /**
   * Constructs a template with an explicit log level, context-fill behavior, and dot-notation pattern.
   *
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param pattern               the dot-notation pattern used to match logger names
   * @throws LoggerException if {@code pattern} cannot be parsed
   */
  public ClassNameTemplate (Level level, boolean autoFillLoggerContext, String pattern)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    setPattern(pattern);
  }

  /**
   * Constructs a template with a log level, context-fill behavior, dot-notation pattern, and appenders.
   *
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param pattern               the dot-notation pattern used to match logger names
   * @param appenders             the appenders to attach to every logger matched by this template
   * @throws LoggerException if {@code pattern} cannot be parsed
   */
  public ClassNameTemplate (Level level, boolean autoFillLoggerContext, String pattern, Appender... appenders)
    throws LoggerException {

    super(level, autoFillLoggerContext, appenders);

    setPattern(pattern);
  }

  /**
   * Constructs a fully specified template with filters, appenders, enhancers, level, context behavior,
   * and a dot-notation pattern.
   *
   * @param filters               filters applied before a record is forwarded to appenders
   * @param appenders             appenders that receive matching records
   * @param enhancers             enhancers that decorate records before they are appended
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param pattern               the dot-notation pattern used to match logger names
   * @throws LoggerException if {@code pattern} cannot be parsed
   */
  public ClassNameTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String pattern)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    setPattern(pattern);
  }

  /**
   * Replaces the current dot-notation pattern with a new one.
   *
   * @param pattern the new dot-notation pattern to use for matching logger names
   * @throws LoggerException if {@code pattern} cannot be parsed by {@link org.smallmind.nutsnbolts.util.DotNotation}
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
   * Computes a match score for the given logger name by delegating to
   * {@link org.smallmind.nutsnbolts.util.DotNotation#calculateValue}, returning {@link Template#NO_MATCH}
   * when the name does not match the configured pattern.
   *
   * @param loggerName the name of the logger being matched
   * @return a positive match score if the name matches the pattern, or {@link Template#NO_MATCH} otherwise
   */
  @Override
  public int matchLogger (String loggerName) {

    return notation.calculateValue(loggerName, NO_MATCH);
  }
}
