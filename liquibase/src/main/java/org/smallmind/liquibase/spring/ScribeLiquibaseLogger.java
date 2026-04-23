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
package org.smallmind.liquibase.spring;

import java.util.logging.Level;
import liquibase.logging.core.AbstractLogger;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Liquibase {@code Logger} implementation that forwards log events to the SmallMind Scribe system.
 *
 * <p>Liquibase uses {@link java.util.logging.Level} (JUL) for its log levels. This class translates
 * each JUL level to its closest Scribe equivalent and delegates to a per-class Scribe
 * {@link Logger} obtained from {@link LoggerManager}. Instances are created by
 * {@link ScribeLogService} and should not normally be constructed directly.</p>
 *
 * <p>JUL-to-Scribe level mapping:</p>
 * <ul>
 *   <li>{@link Level#ALL}, {@link Level#FINEST}, {@link Level#FINER} → {@code TRACE}</li>
 *   <li>{@link Level#FINE} → {@code DEBUG}</li>
 *   <li>{@link Level#CONFIG}, {@link Level#INFO} → {@code INFO}</li>
 *   <li>{@link Level#WARNING} → {@code WARN}</li>
 *   <li>{@link Level#SEVERE} → {@code ERROR}</li>
 *   <li>{@link Level#OFF} → {@code OFF}</li>
 *   <li>Any unrecognised level → {@code INFO}</li>
 *   <li>{@code null} → {@code null} (the Scribe logger will suppress the entry)</li>
 * </ul>
 */
public class ScribeLiquibaseLogger extends AbstractLogger {

  private final Logger scribeLogger;

  /**
   * Creates a logger that emits events tagged with the given class.
   *
   * @param clazz the class whose name is used to identify log entries in the Scribe output;
   *              must not be {@code null}
   */
  public ScribeLiquibaseLogger (Class<?> clazz) {

    scribeLogger = LoggerManager.getLogger(clazz);
  }

  /**
   * Forwards a Liquibase log event to the Scribe logger after translating the level.
   *
   * <p>If {@code level} is {@code null}, {@link #translateLevel(Level)} returns {@code null}
   * and the Scribe logger silently discards the entry.</p>
   *
   * @param level   the JUL severity level supplied by Liquibase; may be {@code null}
   * @param message the log message to emit; may be {@code null}
   * @param e       an optional exception to attach to the log entry; may be {@code null}
   */
  @Override
  public void log (Level level, String message, Throwable e) {

    scribeLogger.log(translateLevel(level), e, message);
  }

  /**
   * Translates a {@link java.util.logging.Level} value to the corresponding Scribe level.
   *
   * <p>Unknown non-null levels default to {@code INFO} to ensure log events are never silently
   * dropped by an unrecognised mapping.</p>
   *
   * @param level the JUL level to translate; may be {@code null}
   * @return the corresponding {@link org.smallmind.scribe.pen.Level}, or {@code null}
   * when {@code level} is {@code null}
   */
  private org.smallmind.scribe.pen.Level translateLevel (java.util.logging.Level level) {

    if (level == null) {
      return null;
    } else if (level.equals(java.util.logging.Level.ALL)) {
      return org.smallmind.scribe.pen.Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINEST)) {
      return org.smallmind.scribe.pen.Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINER)) {
      return org.smallmind.scribe.pen.Level.TRACE;
    } else if (level.equals(java.util.logging.Level.FINE)) {
      return org.smallmind.scribe.pen.Level.DEBUG;
    } else if (level.equals(java.util.logging.Level.CONFIG)) {
      return org.smallmind.scribe.pen.Level.INFO;
    } else if (level.equals(java.util.logging.Level.INFO)) {
      return org.smallmind.scribe.pen.Level.INFO;
    } else if (level.equals(java.util.logging.Level.WARNING)) {
      return org.smallmind.scribe.pen.Level.WARN;
    } else if (level.equals(java.util.logging.Level.SEVERE)) {
      return org.smallmind.scribe.pen.Level.ERROR;
    } else if (level.equals(java.util.logging.Level.OFF)) {
      return org.smallmind.scribe.pen.Level.OFF;
    } else {
      return org.smallmind.scribe.pen.Level.INFO;
    }
  }
}
