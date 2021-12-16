/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.scribe.ink.jdk;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

public class JDKLevelTranslator {

  public static Level getLevel (java.util.logging.Level level) {

    if (level == null) {
      return null;
    } else if (level.equals(java.util.logging.Level.ALL)) {
      return Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINEST)) {
      return Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINER)) {
      return Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINE)) {
      return Level.DEBUG;
    } else if (level.equals(java.util.logging.Level.CONFIG)) {
      return Level.INFO;
    } else if (level.equals(java.util.logging.Level.INFO)) {
      return Level.INFO;
    } else if (level.equals(java.util.logging.Level.WARNING)) {
      return Level.WARN;
    } else if (level.equals(java.util.logging.Level.SEVERE)) {
      return Level.ERROR;
    } else if (level.equals(java.util.logging.Level.OFF)) {
      return Level.OFF;
    } else {
      throw new UnknownSwitchCaseException(level.getLocalizedName());
    }
  }

  public static java.util.logging.Level getJDKLevel (Level level) {

    switch (level) {
      case TRACE:
        return java.util.logging.Level.FINER;
      case DEBUG:
        return java.util.logging.Level.FINE;
      case INFO:
        return java.util.logging.Level.INFO;
      case WARN:
        return java.util.logging.Level.WARNING;
      case ERROR:
        return java.util.logging.Level.SEVERE;
      case FATAL:
        return java.util.logging.Level.SEVERE;
      case OFF:
        return java.util.logging.Level.OFF;
      default:
        throw new UnknownSwitchCaseException(level.name());
    }
  }
}