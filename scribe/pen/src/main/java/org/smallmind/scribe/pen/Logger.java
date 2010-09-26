/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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

import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprintsFactory;
import org.smallmind.scribe.pen.probe.Instrument;
import org.smallmind.scribe.pen.probe.InstrumentAndReturn;
import org.smallmind.scribe.pen.probe.Probe;
import org.smallmind.scribe.pen.probe.ProbeException;
import org.smallmind.scribe.pen.probe.ProbeFactory;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class Logger {

   private LoggerAdapter loggerAdapter;

   public Logger (Class loggableClass) {

      this(loggableClass.getCanonicalName());
   }

   public Logger (String name) {

      loggerAdapter = LoggingBlueprintsFactory.getLoggingBlueprints().getLoggingAdapter(name);
   }

   public String getName () {

      return loggerAdapter.getName();
   }

   public Probe createProbe (Discriminator discriminator, Level level, String title) {

      return ProbeFactory.createProbe(this, discriminator, (level == null) ? getLevel() : level, title);
   }

   public void executeInstrumentation (Instrument instrument)
      throws ProbeException {

      executeInstrumentation(null, null, null, instrument);
   }

   public void executeInstrumentation (Discriminator discriminator, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(discriminator, null, null, instrument);
   }

   public void executeInstrumentation (Level level, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(null, level, null, instrument);
   }

   public void executeInstrumentation (String title, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(null, null, title, instrument);
   }

   public void executeInstrumentation (Discriminator discriminator, String title, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(discriminator, null, title, instrument);
   }

   public void executeInstrumentation (Level level, String title, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(null, level, title, instrument);
   }

   public void executeInstrumentation (Discriminator discriminator, Level level, Instrument instrument)
      throws ProbeException {

      executeInstrumentation(discriminator, level, null, instrument);
   }

   public void executeInstrumentation (Discriminator discriminator, Level level, String title, Instrument instrument)
      throws ProbeException {

      ProbeFactory.executeInstrumentation(this, discriminator, (level == null) ? getLevel() : level, title, instrument);
   }

   public <T> T executeInstrumentationAndReturn (InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(null, null, null, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Discriminator discriminator, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(discriminator, null, null, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Level level, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(null, level, null, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (String title, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(null, null, title, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Discriminator discriminator, String title, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(discriminator, null, title, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Level level, String title, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(null, level, title, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Discriminator discriminator, Level level, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return executeInstrumentationAndReturn(discriminator, level, null, instrumentAndReturn);
   }

   public <T> T executeInstrumentationAndReturn (Discriminator discriminator, Level level, String title, InstrumentAndReturn<T> instrumentAndReturn)
      throws ProbeException {

      return ProbeFactory.executeInstrumentationAndReturn(this, discriminator, (level == null) ? getLevel() : level, title, instrumentAndReturn);
   }

   public Template getTemplate () {

      return LoggerManager.getTemplate(this);
   }

   public boolean getAutoFillLogicalContext () {

      return loggerAdapter.getAutoFillLogicalContext();
   }

   public void setAutoFillLogigicalContext (boolean autoFillLogicalContext) {

      loggerAdapter.setAutoFillLogigicalContext(autoFillLogicalContext);
   }

   public void addFilters (Filter[] filters) {

      for (Filter filter : filters) {
         addFilter(filter);
      }
   }

   public void addFilter (Filter filter) {

      loggerAdapter.addFilter(filter);
   }

   public void clearFilters () {

      loggerAdapter.clearFilters();
   }

   public void addAppenders (Appender[] appenders) {

      for (Appender appender : appenders) {
         addAppender(appender);
      }
   }

   public void addAppender (Appender appender) {

      loggerAdapter.addAppender(appender);
   }

   public void clearAppenders () {

      loggerAdapter.clearAppenders();
   }

   public void addEnhancer (Enhancer enhancer) {

      loggerAdapter.addEnhancer(enhancer);
   }

   public void clearEnhancers () {

      loggerAdapter.clearEnhancers();
   }

   public Level getLevel () {

      return loggerAdapter.getLevel();
   }

   public void setLevel (Level level) {

      if (level == null) {
         throw new IllegalArgumentException("Can't set a 'null' default level");
      }

      loggerAdapter.setLevel(level);
   }

   public void trace (Throwable throwable) {

      log(Level.TRACE, throwable);
   }

   public void trace (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.TRACE, throwable);
   }

   public void trace (String message, Object... args) {

      log(Level.TRACE, message, args);
   }

   public void trace (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.TRACE, message, args);
   }

   public void trace (Throwable throwable, String message, Object... args) {

      log(Level.TRACE, throwable, message, args);
   }

   public void trace (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.TRACE, throwable, message, args);
   }

   public void trace (Object object) {

      log(Level.TRACE, object);
   }

   public void trace (Discriminator discriminator, Object object) {

      log(discriminator, Level.TRACE, object);
   }

   public void trace (Throwable throwable, Object object) {

      log(Level.TRACE, throwable, object);
   }

   public void trace (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.TRACE, throwable, object);
   }

   public void debug (Throwable throwable) {

      log(Level.DEBUG, throwable);
   }

   public void debug (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.DEBUG, throwable);
   }

   public void debug (String message, Object... args) {

      log(Level.DEBUG, message, args);
   }

   public void debug (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.DEBUG, message, args);
   }

   public void debug (Throwable throwable, String message, Object... args) {

      log(Level.DEBUG, throwable, message, args);
   }

   public void debug (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.DEBUG, throwable, message, args);
   }

   public void debug (Object object) {

      log(Level.DEBUG, object);
   }

   public void debug (Discriminator discriminator, Object object) {

      log(discriminator, Level.DEBUG, object);
   }

   public void debug (Throwable throwable, Object object) {

      log(Level.DEBUG, throwable, object);
   }

   public void debug (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.DEBUG, throwable, object);
   }

   public void info (Throwable throwable) {

      log(Level.INFO, throwable);
   }

   public void info (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.INFO, throwable);
   }

   public void info (String message, Object... args) {

      log(Level.INFO, message, args);
   }

   public void info (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.INFO, message, args);
   }

   public void info (Throwable throwable, String message, Object... args) {

      log(Level.INFO, throwable, message, args);
   }

   public void info (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.INFO, throwable, message, args);
   }

   public void info (Object object) {

      log(Level.INFO, object);
   }

   public void info (Discriminator discriminator, Object object) {

      log(discriminator, Level.INFO, object);
   }

   public void info (Throwable throwable, Object object) {

      log(Level.INFO, throwable, object);
   }

   public void info (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.INFO, throwable, object);
   }

   public void warn (Throwable throwable) {

      log(Level.WARN, throwable);
   }

   public void warn (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.WARN, throwable);
   }

   public void warn (String message, Object... args) {

      log(Level.WARN, message, args);
   }

   public void warn (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.WARN, message, args);
   }

   public void warn (Throwable throwable, String message, Object... args) {

      log(Level.WARN, throwable, message, args);
   }

   public void warn (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.WARN, throwable, message, args);
   }

   public void warn (Object object) {

      log(Level.WARN, object);
   }

   public void warn (Discriminator discriminator, Object object) {

      log(discriminator, Level.WARN, object);
   }

   public void warn (Throwable throwable, Object object) {

      log(Level.WARN, throwable, object);
   }

   public void warn (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.WARN, throwable, object);
   }

   public void error (Throwable throwable) {

      log(Level.ERROR, throwable);
   }

   public void error (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.ERROR, throwable);
   }

   public void error (String message, Object... args) {

      log(Level.ERROR, message, args);
   }

   public void error (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.ERROR, message, args);
   }

   public void error (Throwable throwable, String message, Object... args) {

      log(Level.ERROR, throwable, message, args);
   }

   public void error (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.ERROR, throwable, message, args);
   }

   public void error (Object object) {

      log(Level.ERROR, object);
   }

   public void error (Discriminator discriminator, Object object) {

      log(discriminator, Level.ERROR, object);
   }

   public void error (Throwable throwable, Object object) {

      log(Level.ERROR, throwable, object);
   }

   public void error (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.ERROR, throwable, object);
   }

   public void fatal (Throwable throwable) {

      log(Level.FATAL, throwable);
   }

   public void fatal (Discriminator discriminator, Throwable throwable) {

      log(discriminator, Level.FATAL, throwable);
   }

   public void fatal (String message, Object... args) {

      log(Level.FATAL, message, args);
   }

   public void fatal (Discriminator discriminator, String message, Object... args) {

      log(discriminator, Level.FATAL, message, args);
   }

   public void fatal (Throwable throwable, String message, Object... args) {

      log(Level.FATAL, throwable, message, args);
   }

   public void fatal (Discriminator discriminator, Throwable throwable, String message, Object... args) {

      log(discriminator, Level.FATAL, throwable, message, args);
   }

   public void fatal (Object object) {

      log(Level.FATAL, object);
   }

   public void fatal (Discriminator discriminator, Object object) {

      log(discriminator, Level.FATAL, object);
   }

   public void fatal (Throwable throwable, Object object) {

      log(Level.FATAL, throwable, object);
   }

   public void fatal (Discriminator discriminator, Throwable throwable, Object object) {

      log(discriminator, Level.FATAL, throwable, object);
   }

   public void log (Level level, Throwable throwable) {

      loggerAdapter.logMessage(null, (level == null) ? getLevel() : level, throwable, null);
   }

   public void log (Discriminator discriminator, Level level, Throwable throwable) {

      loggerAdapter.logMessage(discriminator, (level == null) ? getLevel() : level, throwable, null);
   }

   public void log (Level level, String message, Object... args) {

      loggerAdapter.logMessage(null, (level == null) ? getLevel() : level, null, message, args);
   }

   public void log (Discriminator discriminator, Level level, String message, Object... args) {

      loggerAdapter.logMessage(discriminator, (level == null) ? getLevel() : level, null, message, args);
   }

   public void log (Level level, Throwable throwable, String message, Object... args) {

      loggerAdapter.logMessage(null, (level == null) ? getLevel() : level, throwable, message, args);
   }

   public void log (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args) {

      loggerAdapter.logMessage(discriminator, (level == null) ? getLevel() : level, throwable, message, args);
   }

   public void log (Level level, ProbeReport probeReport) {

      loggerAdapter.logProbe(null, (level == null) ? getLevel() : level, null, probeReport);
   }

   public void log (Discriminator discriminator, Level level, ProbeReport probeReport) {

      loggerAdapter.logProbe(discriminator, (level == null) ? getLevel() : level, null, probeReport);
   }

   public void log (Level level, Throwable throwable, ProbeReport probeReport) {

      loggerAdapter.logProbe(null, (level == null) ? getLevel() : level, throwable, probeReport);
   }

   public void log (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport) {

      loggerAdapter.logProbe(discriminator, (level == null) ? getLevel() : level, throwable, probeReport);
   }

   public void log (Level level, Object object) {

      loggerAdapter.logMessage(null, (level == null) ? getLevel() : level, null, object);
   }

   public void log (Discriminator discriminator, Level level, Object object) {

      loggerAdapter.logMessage(discriminator, (level == null) ? getLevel() : level, null, object);
   }

   public void log (Level level, Throwable throwable, Object object) {

      loggerAdapter.logMessage(null, (level == null) ? getLevel() : level, throwable, object);
   }

   public void log (Discriminator discriminator, Level level, Throwable throwable, Object object) {

      loggerAdapter.logMessage(discriminator, (level == null) ? getLevel() : level, throwable, object);
   }
}
