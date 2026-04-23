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
package org.smallmind.claxon.emitter.message;

import java.util.function.Consumer;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link Consumer}{@code <String>} implementation that forwards each string to a Scribe
 * {@link Logger} at a configurable log {@link Level}.
 *
 * <p>Instances of this class are intended to be used as the output consumer for a
 * {@link MessageEmitter}, bridging the emitter's text output to the Scribe logging framework.
 * The logger is obtained from {@link LoggerManager} either for this class itself or for a
 * caller-supplied class.
 */
public class ScribeLoggerConsumer implements Consumer<String> {

  /**
   * The Scribe {@link Logger} to which messages are forwarded.
   */
  private final Logger logger;

  /**
   * The {@link Level} at which each message is logged.
   */
  private final Level level;

  /**
   * Creates a consumer that logs messages using the logger registered for
   * {@link ScribeLoggerConsumer} itself.
   *
   * @param level the {@link Level} at which messages will be logged; must not be {@code null}
   */
  public ScribeLoggerConsumer (Level level) {

    this(null, level);
  }

  /**
   * Creates a consumer that logs messages using the logger registered for the specified caller
   * class.
   *
   * <p>When {@code caller} is {@code null}, the logger for {@link ScribeLoggerConsumer} is
   * used instead.
   *
   * @param caller the class whose Scribe logger will be used; may be {@code null}
   * @param level  the {@link Level} at which messages will be logged; must not be {@code null}
   */
  public ScribeLoggerConsumer (Class<?> caller, Level level) {

    this.level = level;

    logger = LoggerManager.getLogger((caller == null) ? ScribeLoggerConsumer.class : caller);
  }

  /**
   * Logs the supplied message at the configured level via the Scribe logger.
   *
   * @param message the metric message to log; must not be {@code null}
   */
  @Override
  public void accept (String message) {

    logger.log(level, message);
  }
}
