/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.scribe.ink.log4j;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

public class Log4JLevelTranslator {

  public static Level getLevel (org.apache.log4j.Level level) {

    if (level == null) {
      return null;
    }
    else if (level.equals(org.apache.log4j.Level.TRACE)) {
      return Level.TRACE;
    }
    else if (level.equals(org.apache.log4j.Level.DEBUG)) {
      return Level.DEBUG;
    }
    else if (level.equals(org.apache.log4j.Level.INFO)) {
      return Level.INFO;
    }
    else if (level.equals(org.apache.log4j.Level.WARN)) {
      return Level.WARN;
    }
    else if (level.equals(org.apache.log4j.Level.ERROR)) {
      return Level.ERROR;
    }
    else if (level.equals(org.apache.log4j.Level.FATAL)) {
      return Level.FATAL;
    }
    else if (level.equals(org.apache.log4j.Level.OFF)) {
      return Level.OFF;
    }
    else {
      throw new UnknownSwitchCaseException(String.valueOf(level.toInt()));
    }
  }

  public static org.apache.log4j.Level getLog4JLevel (Level level) {

    switch (level) {
      case TRACE:
        return org.apache.log4j.Level.TRACE;
      case DEBUG:
        return org.apache.log4j.Level.DEBUG;
      case INFO:
        return org.apache.log4j.Level.INFO;
      case WARN:
        return org.apache.log4j.Level.WARN;
      case ERROR:
        return org.apache.log4j.Level.ERROR;
      case FATAL:
        return org.apache.log4j.Level.FATAL;
      case OFF:
        return org.apache.log4j.Level.OFF;
      default:
        throw new UnknownSwitchCaseException(level.name());
    }
  }
}
