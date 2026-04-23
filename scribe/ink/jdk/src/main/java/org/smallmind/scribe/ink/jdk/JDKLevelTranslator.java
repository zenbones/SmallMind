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
package org.smallmind.scribe.ink.jdk;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

/**
 * Stateless utility class that converts between scribe {@link Level} values and JUL
 * {@link java.util.logging.Level} constants, mapping FATAL to SEVERE and ALL/FINEST/FINER to TRACE.
 */
public class JDKLevelTranslator {

  /**
   * Translates a JUL {@link java.util.logging.Level} to its scribe {@link Level} equivalent, mapping
   * {@code ALL}, {@code FINEST}, and {@code FINER} to {@link Level#TRACE} and {@code SEVERE} to
   * {@link Level#ERROR}.
   *
   * @param level the JUL level to translate; {@code null} returns {@code null}
   * @return the corresponding scribe level, or {@code null} if {@code level} is {@code null}
   * @throws UnknownSwitchCaseException if the JUL level is not one of the known constants
   */
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

  /**
   * Translates a scribe {@link Level} to its JUL {@link java.util.logging.Level} equivalent, mapping
   * both {@link Level#FATAL} and {@link Level#ERROR} to {@link java.util.logging.Level#SEVERE}.
   *
   * @param level the scribe level to translate
   * @return the corresponding JUL level
   * @throws UnknownSwitchCaseException if the scribe level is not a recognised switch case
   */
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
