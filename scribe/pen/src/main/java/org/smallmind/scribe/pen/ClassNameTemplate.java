/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

public class ClassNameTemplate extends Template {

  private AtomicReference<DotNotation> notationRef = new AtomicReference<DotNotation>();

  public ClassNameTemplate () {

    super();
  }

  public ClassNameTemplate (String pattern)
    throws LoggerException {

    super();

    try {
      notationRef.set(new DotNotation(pattern));
    }
    catch (DotNotationException dotNotationException) {
      throw new LoggerException(dotNotationException);
    }
  }

  public ClassNameTemplate (Level level, boolean autoFillLogicalContext, String pattern)
    throws LoggerException {

    super(level, autoFillLogicalContext);

    try {
      notationRef.set(new DotNotation(pattern));
    }
    catch (DotNotationException dotNotationException) {
      throw new LoggerException(dotNotationException);
    }
  }

  public ClassNameTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLogicalContext, String pattern)
    throws LoggerException {

    super(filters, appenders, enhancers, level, autoFillLogicalContext);

    try {
      notationRef.set(new DotNotation(pattern));
    }
    catch (DotNotationException dotNotationException) {
      throw new LoggerException(dotNotationException);
    }
  }

  public void setPattern (String pattern)
    throws LoggerException {

    try {
      if (!notationRef.compareAndSet(null, new DotNotation(pattern))) {
        throw new LoggerRuntimeException("ClassNameTemplate has been previously initialized with a pattern");
      }
    }
    catch (DotNotationException dotNotationException) {
      throw new LoggerException(dotNotationException);
    }
  }

  public int matchLogger (String loggerName) {

    Matcher matcher;
    Integer[] dotPositions;
    int matchValue = NO_MATCH;

    if (notationRef.get() == null) {
      throw new LoggerRuntimeException("ClassNameTemplate was never initialized with a pattern");
    }

    dotPositions = getDotPositions(loggerName);
    matcher = notationRef.get().getPattern().matcher(loggerName);

    if (matcher.matches()) {
      matchValue += 2;
      for (int count = 1; count <= matcher.groupCount(); count++) {
        matchValue += assignValueToMatch(dotPositions, matcher.start(count));
      }
    }

    return matchValue;
  }

  private static int assignValueToMatch (Integer[] dotPositions, int matchStart) {

    int index = 0;

    for (Integer dotPosition : dotPositions) {
      if (dotPosition > matchStart) {
        break;
      }
      index++;
    }

    return (int)Math.pow(2, index);
  }

  private static Integer[] getDotPositions (String loggerName) {

    Integer[] dotPosiitions;
    LinkedList<Integer> dotPositionList;

    dotPositionList = new LinkedList<Integer>();
    for (int count = 0; count < loggerName.length(); count++) {
      if (loggerName.charAt(count) == '.') {
        dotPositionList.add(count);
      }
    }

    dotPosiitions = new Integer[dotPositionList.size()];
    dotPositionList.toArray(dotPosiitions);

    return dotPosiitions;
  }
}