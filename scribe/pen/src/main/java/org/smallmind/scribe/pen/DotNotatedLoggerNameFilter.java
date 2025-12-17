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

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

/**
 * Filter that enables logging for specific logger names matched via dot-notation patterns, while passing through higher-level events.
 */
public class DotNotatedLoggerNameFilter implements Filter {

  private final HashMap<String, DotNotation> patternMap = new HashMap<>();
  private final ConcurrentLinkedQueue<String> classList = new ConcurrentLinkedQueue<>();

  private final Lock patternReadLock;
  private final Lock patternWriteLock;
  private Level passThroughLevel;

  /**
   * Creates a filter with default pass-through level INFO.
   *
   * @throws LoggerException if pattern initialization fails
   */
  public DotNotatedLoggerNameFilter ()
    throws LoggerException {

    this(Level.INFO, null);
  }

  /**
   * Creates a filter with a specific pass-through level.
   *
   * @param passThroughLevel minimum level that always passes
   * @throws LoggerException if pattern initialization fails
   */
  public DotNotatedLoggerNameFilter (Level passThroughLevel)
    throws LoggerException {

    this(passThroughLevel, null);
  }

  /**
   * Creates a filter with a pass-through level and initial patterns.
   *
   * @param passThroughLevel minimum level that always passes
   * @param patterns         logger name patterns to enable
   * @throws LoggerException if pattern initialization fails
   */
  public DotNotatedLoggerNameFilter (Level passThroughLevel, List<String> patterns)
    throws LoggerException {

    ReadWriteLock patternReadWriteLock;

    this.passThroughLevel = passThroughLevel;

    if (patterns != null) {
      setPatterns(patterns);
    }

    patternReadWriteLock = new ReentrantReadWriteLock();
    patternReadLock = patternReadWriteLock.readLock();
    patternWriteLock = patternReadWriteLock.writeLock();
  }

  /**
   * Retrieves the configured pass-through level.
   *
   * @return minimum level that always passes
   */
  public synchronized Level getPassThroughLevel () {

    return passThroughLevel;
  }

  /**
   * Sets the level at which records automatically pass through the filter.
   *
   * @param passThroughLevel minimum level that always passes
   */
  public synchronized void setPassThroughLevel (Level passThroughLevel) {

    this.passThroughLevel = passThroughLevel;
  }

  /**
   * Replaces all patterns used to enable logging for matching class names.
   *
   * @param patterns dot-notation patterns
   * @throws LoggerException if any pattern is invalid
   */
  public synchronized void setPatterns (List<String> patterns)
    throws LoggerException {

    patternMap.clear();

    for (String protoPattern : patterns) {
      try {
        patternMap.put(protoPattern, new DotNotation(protoPattern));
      } catch (DotNotationException dotNotationException) {
        throw new LoggerException(dotNotationException);
      }
    }
  }

  /**
   * Determines whether logging is enabled for the given class name.
   *
   * @param className logger name to test
   * @return {@code true} if matched or previously cached, otherwise {@code false}
   */
  public boolean isClassNameOn (String className) {

    return (className != null) && (classList.contains(className) || noCachedMatch(className, true));
  }

  /**
   * Searches for a pattern match, optionally caching successful matches.
   *
   * @param className  logger name to test
   * @param addIfFound whether to cache the match
   * @return {@code true} if a pattern matches; otherwise {@code false}
   */
  private boolean noCachedMatch (String className, boolean addIfFound) {

    patternReadLock.lock();
    try {
      for (DotNotation notation : patternMap.values()) {
        if (notation.getPattern().matcher(className).matches()) {
          if (addIfFound) {
            synchronized (classList) {
              if (!classList.contains(className)) {
                classList.add(className);
              }
            }
          }

          return true;
        }
      }

      return false;
    } finally {
      patternReadLock.unlock();
    }
  }

  /**
   * Adds or removes a pattern at runtime and updates cached matches accordingly.
   *
   * @param protoPattern pattern to add or remove
   * @param isOn         {@code true} to add, {@code false} to remove
   * @throws LoggerException if the pattern cannot be parsed when adding
   */
  public void setDebugCategory (String protoPattern, boolean isOn)
    throws LoggerException {

    patternWriteLock.lock();
    try {
      if (isOn) {
        if (!patternMap.containsKey(protoPattern)) {
          try {
            patternMap.put(protoPattern, new DotNotation(protoPattern));
          } catch (DotNotationException dotNotationException) {
            throw new LoggerException(dotNotationException);
          }
        }
      } else if (patternMap.remove(protoPattern) != null) {
        classList.removeIf(className -> !noCachedMatch(className, false));
      }
    } finally {
      patternWriteLock.unlock();
    }
  }

  /**
   * Evaluates whether the record should be logged based on level and pattern matches.
   *
   * @param record record to evaluate
   * @return {@code true} if the record passes the level threshold or matches a configured pattern
   */
  public boolean willLog (Record<?> record) {

    return record.getLevel().atLeast(passThroughLevel) || isClassNameOn(record.getLoggerName());
  }
}
