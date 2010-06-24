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
