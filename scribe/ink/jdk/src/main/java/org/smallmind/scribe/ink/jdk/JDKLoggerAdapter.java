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
package org.smallmind.scribe.ink.jdk;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.ParameterAdapter;
import org.smallmind.scribe.pen.adapter.ScribeParameterAdapter;

public class JDKLoggerAdapter implements LoggerAdapter {

  private final Logger logger;

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLoggerContext = false;

  public JDKLoggerAdapter (Logger logger) {

    this.logger = logger;

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  @Override
  public String getName () {

    return logger.getName();
  }

  @Override
  public ParameterAdapter getParameterAdapter () {

    return ScribeParameterAdapter.getInstance();
  }

  @Override
  public boolean getAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  @Override
  public synchronized void addFilter (Filter filter) {

    synchronized (logger) {
      filterList.add(filter);
      logger.setFilter(new JDKFilterWrapper(filter));
    }
  }

  @Override
  public synchronized void clearFilters () {

    filterList.clear();
    logger.setFilter(null);
  }

  @Override
  public synchronized void addAppender (Appender appender) {

    logger.addHandler(new JDKAppenderWrapper(appender));
  }

  @Override
  public synchronized void clearAppenders () {

    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
  }

  @Override
  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  @Override
  public void clearEnhancers () {

    enhancerList.clear();
  }

  @Override
  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : JDKLevelTranslator.getLevel(logger.getLevel());
  }

  @Override
  public void setLevel (Level level) {

    logger.setLevel(JDKLevelTranslator.getLog4JLevel(level));
  }

  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, message, args);
        ((ParameterAwareRecord)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, (object == null) ? null : object.toString());
        ((ParameterAwareRecord)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, (supplier == null) ? null : supplier.get());
        ((ParameterAwareRecord)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  private LoggerContext willLog (Level level) {

    LoggerContext loggerContext;
    Record record;

    loggerContext = new DefaultLoggerContext();
    if (getAutoFillLoggerContext()) {
      loggerContext.fillIn();
    }

    if (!((logger.getFilter() == null) && filterList.isEmpty())) {
      record = new JDKRecordSubverter(logger.getName(), level, loggerContext, null, null).getRecord();

      if (logger.getFilter() != null) {
        if (!logger.getFilter().isLoggable((LogRecord)record.getNativeLogEntry())) {
          return null;
        }
      }

      if (!filterList.isEmpty()) {
        for (Filter filter : filterList) {
          if (!filter.willLog(record)) {
            return null;
          }
        }
      }
    }

    return loggerContext;
  }

  private void enhanceRecord (Record record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}