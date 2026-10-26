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
package org.smallmind.web.grizzly;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.smallmind.scribe.pen.AbstractAppender;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.Record;

/**
 * Collects the records a scribe logger emits so a test can assert on them.
 */
public class CapturingAppender extends AbstractAppender {

  private static final Map<Class<?>, Level> PREVIOUS_LEVELS = new ConcurrentHashMap<>();
  private static final Map<Class<?>, CapturingAppender> ATTACHED = new ConcurrentHashMap<>();

  private final List<Record<?>> records = new CopyOnWriteArrayList<>();
  private volatile boolean released = false;

  /**
   * Adds this appender to the logger for the given class, alongside whatever appenders it already has, and sets the
   * logger to accept records at or above the given level, whether that is lower or higher than before. The logger's
   * previous level is remembered so that
   * {@link #release(Class)} can restore it, and an appender still attached for the class from an earlier call is
   * retired first. Call {@link #release(Class)} when the test is done so the capture does not outlive it.
   *
   * @param loggableClass the class whose logger is captured
   * @param level         the lowest level to capture
   * @return the attached appender
   */
  public static CapturingAppender attach (Class<?> loggableClass, Level level) {

    Logger logger = LoggerManager.getLogger(loggableClass);
    CapturingAppender appender = new CapturingAppender();

    CapturingAppender previousAppender;

    PREVIOUS_LEVELS.putIfAbsent(loggableClass, logger.getLevel());

    if ((previousAppender = ATTACHED.put(loggableClass, appender)) != null) {
      previousAppender.released = true;
    }
    logger.addAppender(appender);
    logger.setLevel(level);

    return appender;
  }

  /**
   * Retires the appender attached for the given class, so it captures nothing further, and restores the level the
   * logger had before the first {@link #attach(Class, Level)} for that class. The logger's own appenders are never
   * touched; scribe offers no way to remove a single appender, so the retired appender stays registered but inert.
   *
   * @param loggableClass the class whose logger is released
   */
  public static void release (Class<?> loggableClass) {

    Logger logger = LoggerManager.getLogger(loggableClass);
    Level previousLevel = PREVIOUS_LEVELS.remove(loggableClass);
    CapturingAppender attachedAppender = ATTACHED.remove(loggableClass);

    if (attachedAppender != null) {
      attachedAppender.released = true;
    }
    if (previousLevel != null) {
      logger.setLevel(previousLevel);
    }
  }

  /**
   * Returns the records captured so far, oldest first.
   *
   * @return an unmodifiable view of the captured records
   */
  public List<Record<?>> getRecords () {

    return Collections.unmodifiableList(records);
  }

  /**
   * Returns the number of records captured so far.
   *
   * @return the number of captured records
   */
  public int size () {

    return records.size();
  }

  @Override
  public void handleOutput (Record<?> record) {

    if (!released) {
      records.add(record);
    }
  }
}
