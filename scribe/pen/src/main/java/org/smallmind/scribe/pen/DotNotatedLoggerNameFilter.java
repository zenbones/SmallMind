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
 * A {@link Filter} that selectively enables logging for logger names that match dot-notation patterns
 * while automatically passing through any record whose level meets or exceeds a configurable threshold.
 * Matched class names are cached in a concurrent queue to avoid redundant pattern evaluation.
 */
public class DotNotatedLoggerNameFilter implements Filter {

  private final HashMap<String, DotNotation> patternMap = new HashMap<>();
  private final ConcurrentLinkedQueue<String> classList = new ConcurrentLinkedQueue<>();

  private final Lock patternReadLock;
  private final Lock patternWriteLock;
  private Level passThroughLevel;

  /**
   * Constructs a filter with a pass-through level of {@link Level#INFO} and no initial patterns.
   *
   * @throws LoggerException if internal initialization fails
   */
  public DotNotatedLoggerNameFilter ()
    throws LoggerException {

    this(Level.INFO, null);
  }

  /**
   * Constructs a filter that automatically passes records at or above {@code passThroughLevel}
   * and applies no initial dot-notation patterns.
   *
   * @param passThroughLevel the minimum level at which records pass regardless of logger name
   * @throws LoggerException if internal initialization fails
   */
  public DotNotatedLoggerNameFilter (Level passThroughLevel)
    throws LoggerException {

    this(passThroughLevel, null);
  }

  /**
   * Constructs a filter with an explicit pass-through level and a set of initial dot-notation patterns.
   *
   * @param passThroughLevel the minimum level at which records pass regardless of logger name
   * @param patterns         initial dot-notation patterns used to match logger names; may be {@code null}
   * @throws LoggerException if any pattern in {@code patterns} cannot be parsed
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
   * Returns the level at or above which records are automatically allowed through without pattern matching.
   *
   * @return the current pass-through level
   */
  public synchronized Level getPassThroughLevel () {

    return passThroughLevel;
  }

  /**
   * Replaces the pass-through level; records at or above this level will always be logged
   * regardless of whether their logger name matches any pattern.
   *
   * @param passThroughLevel the new minimum level for unconditional pass-through
   */
  public synchronized void setPassThroughLevel (Level passThroughLevel) {

    this.passThroughLevel = passThroughLevel;
  }

  /**
   * Replaces the entire set of active dot-notation patterns, clearing any previously cached class-name matches.
   *
   * @param patterns the new list of dot-notation patterns to install
   * @throws LoggerException if any pattern in the list cannot be parsed by {@link org.smallmind.nutsnbolts.util.DotNotation}
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
   * Returns {@code true} if the given class name is already in the match cache or is matched by
   * at least one of the configured dot-notation patterns, adding it to the cache on first match.
   *
   * @param className the logger name to test; returns {@code false} if {@code null}
   * @return {@code true} if the class name is enabled; {@code false} otherwise
   */
  public boolean isClassNameOn (String className) {

    return (className != null) && (classList.contains(className) || noCachedMatch(className, true));
  }

  /**
   * Evaluates all configured patterns against the class name under the read lock, optionally
   * inserting the name into the cache when a match is found.
   *
   * @param className  the logger name to test against the pattern map
   * @param addIfFound {@code true} to add the name to the cache upon a successful match
   * @return {@code true} if at least one pattern matches; {@code false} otherwise
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
   * Dynamically adds or removes a single dot-notation pattern from the active set, updating the
   * class-name cache accordingly; when a pattern is removed, cached names that no longer match
   * any remaining pattern are evicted from the cache.
   *
   * @param protoPattern the dot-notation pattern string to add or remove
   * @param isOn         {@code true} to add the pattern; {@code false} to remove it
   * @throws LoggerException if {@code isOn} is {@code true} and the pattern cannot be parsed
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
   * Returns {@code true} if the record's level meets or exceeds the pass-through level, or if
   * the record's logger name matches one of the configured dot-notation patterns.
   *
   * @param record the log record to evaluate
   * @return {@code true} if the record should be logged; {@code false} otherwise
   */
  public boolean willLog (Record<?> record) {

    return record.getLevel().atLeast(passThroughLevel) || isClassNameOn(record.getLoggerName());
  }
}
