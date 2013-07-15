/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public class RegExpTemplate extends Template {

  private AtomicReference<Pattern> loggerPatternRef = new AtomicReference<Pattern>();

  public RegExpTemplate () {

    super();
  }

  public RegExpTemplate (String expression) {

    super();

    loggerPatternRef.set(Pattern.compile(expression));
  }

  public RegExpTemplate (Level level, boolean autoFillLogicalContext, String expression)
    throws LoggerException {

    super(level, autoFillLogicalContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  public RegExpTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLogicalContext, String expression)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLogicalContext);

    loggerPatternRef.set(Pattern.compile(expression));
  }

  public void setExpression (String expression) {

    if (!loggerPatternRef.compareAndSet(null, Pattern.compile(expression))) {
      throw new LoggerRuntimeException("RegExpTemplate has been previously initialized with a pattern");
    }
  }

  public int matchLogger (String loggerName) {

    if (loggerPatternRef.get() == null) {
      throw new LoggerRuntimeException("RegExpTemplate was never initialized with a pattern");
    }

    return loggerPatternRef.get().matcher(loggerName).matches() ? Integer.MAX_VALUE : NO_MATCH;
  }
}