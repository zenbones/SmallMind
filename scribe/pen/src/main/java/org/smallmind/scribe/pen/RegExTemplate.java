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
import java.util.regex.Pattern;

/**
 * A {@link Template} that matches logger names against a regular expression with the highest possible
 * priority ({@code Integer.MAX_VALUE}); the compiled {@link Pattern} is stored in an
 * {@link java.util.concurrent.atomic.AtomicReference} to guarantee single initialization.
 */
public class RegExTemplate extends Template {

  private final AtomicReference<Pattern> loggerPatternRef = new AtomicReference<Pattern>();

  /**
   * Constructs an uninitialized template; {@link #setExpression(String)} must be called exactly
   * once before this template is used.
   */
  public RegExTemplate () {

    super();
  }

  /**
   * Constructs a template that matches logger names against the given regular expression.
   *
   * @param expression the regular expression to compile and match against logger names
   */
  public RegExTemplate (String expression) {

    super();

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Constructs a template with an explicit log level, context-fill behavior, and regular expression.
   *
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param expression            the regular expression to compile and match against logger names
   * @throws LoggerException if internal initialization fails
   */
  public RegExTemplate (Level level, boolean autoFillLoggerContext, String expression)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Constructs a fully specified template with filters, appenders, enhancers, level, context behavior,
   * and a regular expression.
   *
   * @param filters               filters applied before a record is forwarded to appenders
   * @param appenders             appenders that receive matching records
   * @param enhancers             enhancers that decorate records before they are appended
   * @param level                 the default {@link Level} for loggers matched by this template
   * @param autoFillLoggerContext {@code true} to automatically capture the caller's context on each record
   * @param expression            the regular expression to compile and match against logger names
   * @throws LoggerException if internal initialization fails
   */
  public RegExTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String expression)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Compiles and stores the regular expression; may only be called once.
   *
   * @param expression the regular expression to compile and use for matching logger names
   * @throws LoggerRuntimeException if this template has already been initialized with an expression
   */
  public void setExpression (String expression) {

    if (!loggerPatternRef.compareAndSet(null, Pattern.compile(expression))) {
      throw new LoggerRuntimeException("RegExpTemplate has been previously initialized with a pattern");
    }
  }

  /**
   * Returns {@code Integer.MAX_VALUE} when the full logger name matches the configured regular expression,
   * giving this template the highest possible priority, or {@link Template#NO_MATCH} for non-matching names.
   *
   * @param loggerName the logger name to evaluate
   * @return {@code Integer.MAX_VALUE} on a full regex match; {@link Template#NO_MATCH} otherwise
   * @throws LoggerRuntimeException if this template was never initialized with an expression
   */
  public int matchLogger (String loggerName) {

    if (loggerPatternRef.get() == null) {
      throw new LoggerRuntimeException("RegExpTemplate was never initialized with a pattern");
    }

    return loggerPatternRef.get().matcher(loggerName).matches() ? Integer.MAX_VALUE : NO_MATCH;
  }
}
