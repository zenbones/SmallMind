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
package org.smallmind.scribe.pen;

import java.io.Serializable;
import java.util.function.Supplier;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprintFactory;

public class Logger {

  private final LoggerAdapter loggerAdapter;

  public Logger (Class loggableClass) {

    this(loggableClass.getCanonicalName());
  }

  public Logger (String name) {

    loggerAdapter = LoggingBlueprintFactory.getLoggingBlueprint().getLoggingAdapter(name);
  }

  public String getName () {

    return loggerAdapter.getName();
  }

  public Template getTemplate () {

    return LoggerManager.getTemplate(this);
  }

  public void putParameter (String key, Serializable value) {

    loggerAdapter.getParameterAdapter().put(key, value);
  }

  public void removeParameter (String key) {

    loggerAdapter.getParameterAdapter().remove(key);
  }

  public void clearParameters () {

    loggerAdapter.getParameterAdapter().clear();
  }

  public Parameter[] getParameters () {

    return loggerAdapter.getParameterAdapter().getParameters();
  }

  public boolean getAutoFillLoggerContext () {

    return loggerAdapter.getAutoFillLoggerContext();
  }

  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    loggerAdapter.setAutoFillLoggerContext(autoFillLoggerContext);
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

  public void trace (String message, Object... args) {

    log(Level.TRACE, message, args);
  }

  public void trace (Throwable throwable, String message, Object... args) {

    log(Level.TRACE, throwable, message, args);
  }

  public void trace (Object object) {

    log(Level.TRACE, object);
  }

  public void trace (Supplier<String> supplier) {

    log(Level.TRACE, supplier);
  }

  public void trace (Throwable throwable, Object object) {

    log(Level.TRACE, throwable, object);
  }

  public void trace (Throwable throwable, Supplier<String> supplier) {

    log(Level.TRACE, throwable, supplier);
  }

  public void debug (Throwable throwable) {

    log(Level.DEBUG, throwable);
  }

  public void debug (String message, Object... args) {

    log(Level.DEBUG, message, args);
  }

  public void debug (Throwable throwable, String message, Object... args) {

    log(Level.DEBUG, throwable, message, args);
  }

  public void debug (Object object) {

    log(Level.DEBUG, object);
  }

  public void debug (Supplier<String> supplier) {

    log(Level.DEBUG, supplier);
  }

  public void debug (Throwable throwable, Object object) {

    log(Level.DEBUG, throwable, object);
  }

  public void debug (Throwable throwable, Supplier<String> supplier) {

    log(Level.DEBUG, throwable, supplier);
  }

  public void info (Throwable throwable) {

    log(Level.INFO, throwable);
  }

  public void info (String message, Object... args) {

    log(Level.INFO, message, args);
  }

  public void info (Throwable throwable, String message, Object... args) {

    log(Level.INFO, throwable, message, args);
  }

  public void info (Object object) {

    log(Level.INFO, object);
  }

  public void info (Supplier<String> supplier) {

    log(Level.INFO, supplier);
  }

  public void info (Throwable throwable, Object object) {

    log(Level.INFO, throwable, object);
  }

  public void info (Throwable throwable, Supplier<String> supplier) {

    log(Level.INFO, throwable, supplier);
  }

  public void warn (Throwable throwable) {

    log(Level.WARN, throwable);
  }

  public void warn (String message, Object... args) {

    log(Level.WARN, message, args);
  }

  public void warn (Throwable throwable, String message, Object... args) {

    log(Level.WARN, throwable, message, args);
  }

  public void warn (Object object) {

    log(Level.WARN, object);
  }

  public void warn (Supplier<String> supplier) {

    log(Level.WARN, supplier);
  }

  public void warn (Throwable throwable, Object object) {

    log(Level.WARN, throwable, object);
  }

  public void warn (Throwable throwable, Supplier<String> supplier) {

    log(Level.WARN, throwable, supplier);
  }

  public void error (Throwable throwable) {

    log(Level.ERROR, throwable);
  }

  public void error (String message, Object... args) {

    log(Level.ERROR, message, args);
  }

  public void error (Throwable throwable, String message, Object... args) {

    log(Level.ERROR, throwable, message, args);
  }

  public void error (Object object) {

    log(Level.ERROR, object);
  }

  public void error (Supplier<String> supplier) {

    log(Level.ERROR, supplier);
  }

  public void error (Throwable throwable, Object object) {

    log(Level.ERROR, throwable, object);
  }

  public void error (Throwable throwable, Supplier<String> supplier) {

    log(Level.ERROR, throwable, supplier);
  }

  public void fatal (Throwable throwable) {

    log(Level.FATAL, throwable);
  }

  public void fatal (String message, Object... args) {

    log(Level.FATAL, message, args);
  }

  public void fatal (Throwable throwable, String message, Object... args) {

    log(Level.FATAL, throwable, message, args);
  }

  public void fatal (Object object) {

    log(Level.FATAL, object);
  }

  public void fatal (Supplier<String> supplier) {

    log(Level.FATAL, supplier);
  }

  public void fatal (Throwable throwable, Object object) {

    log(Level.FATAL, throwable, object);
  }

  public void fatal (Throwable throwable, Supplier<String> supplier) {

    log(Level.FATAL, throwable, supplier);
  }

  public void log (Level level, Throwable throwable) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, null);
  }

  public void log (Level level, String message, Object... args) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, message, args);
  }

  public void log (Level level, Throwable throwable, String message, Object... args) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, message, args);
  }

  public void log (Level level, Object object) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, object);
  }

  public void log (Level level, Supplier<String> supplier) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, null, supplier);
  }

  public void log (Level level, Throwable throwable, Object object) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, object);
  }

  public void log (Level level, Throwable throwable, Supplier<String> supplier) {

    loggerAdapter.logMessage((level == null) ? getLevel() : level, throwable, supplier);
  }
}
