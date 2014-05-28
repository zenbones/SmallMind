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
package org.smallmind.scribe.pen;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

public class DotNotatedLoggerNameFilter implements Filter {

  private final HashMap<String, DotNotation> patternMap = new HashMap<String, DotNotation>();
  private final ConcurrentLinkedQueue<String> classList = new ConcurrentLinkedQueue<String>();

  private Lock patternReadLock;
  private Lock patternWriteLock;
  private Level passThroughLevel;

  public DotNotatedLoggerNameFilter ()
    throws LoggerException {

    this(Level.INFO, null);
  }

  public DotNotatedLoggerNameFilter (Level passThroughLevel)
    throws LoggerException {

    this(passThroughLevel, null);
  }

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

  public synchronized Level getPassThroughLevel () {

    return passThroughLevel;
  }

  public synchronized void setPassThroughLevel (Level passThroughLevel) {

    this.passThroughLevel = passThroughLevel;
  }

  public synchronized void setPatterns (List<String> patterns)
    throws LoggerException {

    patternMap.clear();

    for (String protoPattern : patterns) {
      try {
        patternMap.put(protoPattern, new DotNotation(protoPattern));
      }
      catch (DotNotationException dotNotationException) {
        throw new LoggerException(dotNotationException);
      }
    }
  }

  public boolean isClassNameOn (String className) {

    return (className != null) && (classList.contains(className) || noCachedMatch(className, true));
  }

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
    }
    finally {
      patternReadLock.unlock();
    }
  }

  public void setDebugCategory (String protoPattern, boolean isOn)
    throws LoggerException {

    patternWriteLock.lock();
    try {
      if (isOn) {
        if (!patternMap.containsKey(protoPattern)) {
          try {
            patternMap.put(protoPattern, new DotNotation(protoPattern));
          }
          catch (DotNotationException dotNotationException) {
            throw new LoggerException(dotNotationException);
          }
        }
      }
      else if (patternMap.remove(protoPattern) != null) {
        for (String className : classList) {
          if (!noCachedMatch(className, false)) {
            classList.remove(className);
          }
        }
      }
    }
    finally {
      patternWriteLock.unlock();
    }
  }

  public boolean willLog (Record record) {

    return record.getLevel().atLeast(passThroughLevel) || isClassNameOn(record.getLoggerName());
  }
}
