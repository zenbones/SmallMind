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
 * Template that matches logger names using a regular expression.
 */
public class RegExTemplate extends Template {

  private final AtomicReference<Pattern> loggerPatternRef = new AtomicReference<Pattern>();

  /**
   * Creates an uninitialized regex template; set the expression before use.
   */
  public RegExTemplate () {

    super();
  }

  /**
   * Creates a template with a specific regex expression.
   *
   * @param expression regular expression used to match logger names
   */
  public RegExTemplate (String expression) {

    super();

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Creates a template with level, context behavior, and regex expression.
   *
   * @param level                 default level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param expression            regular expression used to match logger names
   * @throws LoggerException if initialization fails
   */
  public RegExTemplate (Level level, boolean autoFillLoggerContext, String expression)
    throws LoggerException {

    super(level, autoFillLoggerContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Creates a template with filters, appenders, enhancers, level, context behavior, and regex expression.
   *
   * @param filters               filters to apply
   * @param appenders             appenders to attach
   * @param enhancers             enhancers to apply
   * @param level                 default level
   * @param autoFillLoggerContext whether to auto-fill logger context
   * @param expression            regular expression used to match logger names
   * @throws LoggerException if initialization fails
   */
  public RegExTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext, String expression)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLoggerContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  /**
   * Sets the regex expression to match logger names, only if not already initialized.
   *
   * @param expression regular expression used to match logger names
   * @throws LoggerRuntimeException if the pattern was already set
   */
  public void setExpression (String expression) {

    if (!loggerPatternRef.compareAndSet(null, Pattern.compile(expression))) {
      throw new LoggerRuntimeException("RegExpTemplate has been previously initialized with a pattern");
    }
  }

  /**
   * Matches the provided logger name against the configured regex.
   *
   * @param loggerName logger name to evaluate
   * @return {@code Integer.MAX_VALUE} for a match, otherwise {@link Template#NO_MATCH}
   * @throws LoggerRuntimeException if the pattern was never initialized
   */
  public int matchLogger (String loggerName) {

    if (loggerPatternRef.get() == null) {
      throw new LoggerRuntimeException("RegExpTemplate was never initialized with a pattern");
    }

    return loggerPatternRef.get().matcher(loggerName).matches() ? Integer.MAX_VALUE : NO_MATCH;
  }
}
