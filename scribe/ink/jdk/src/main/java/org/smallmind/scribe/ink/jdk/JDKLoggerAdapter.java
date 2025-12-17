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
import org.smallmind.scribe.pen.adapter.Parameters;

/**
 * Logger adapter that routes scribe logging calls to Java Util Logging.
 */
public class JDKLoggerAdapter implements LoggerAdapter {

  private final Logger logger;

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLoggerContext = false;

  /**
   * Creates an adapter around a JUL {@link Logger}.
   *
   * @param logger the JUL logger to delegate to
   */
  public JDKLoggerAdapter (Logger logger) {

    this.logger = logger;

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the underlying logger name.
   *
   * @return the name reported by the JUL logger
   */
  @Override
  public String getName () {

    return logger.getName();
  }

  /**
   * Supplies the parameter adapter used to store MDC-style values.
   *
   * @return the shared {@link ParameterAdapter}
   */
  @Override
  public ParameterAdapter getParameterAdapter () {

    return Parameters.getInstance();
  }

  /**
   * Indicates whether logger context should be auto-populated.
   *
   * @return {@code true} when context auto-fill is enabled
   */
  @Override
  public boolean getAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  /**
   * Toggles automatic population of logger context data.
   *
   * @param autoFillLoggerContext {@code true} to capture context data automatically
   */
  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Adds a scribe filter and installs a JUL filter wrapper.
   *
   * @param filter filter to evaluate records with
   */
  @Override
  public synchronized void addFilter (Filter filter) {

    synchronized (logger) {
      filterList.add(filter);
      logger.setFilter(new JDKFilterWrapper(filter));
    }
  }

  /**
   * Removes all registered filters from both JUL and scribe collections.
   */
  @Override
  public synchronized void clearFilters () {

    filterList.clear();
    logger.setFilter(null);
  }

  /**
   * Adds an appender by wrapping it in a JUL {@link Handler}.
   *
   * @param appender appender to register
   */
  @Override
  public synchronized void addAppender (Appender appender) {

    logger.addHandler(new JDKAppenderWrapper(appender));
  }

  /**
   * Removes all appenders currently attached to the JUL logger.
   */
  @Override
  public synchronized void clearAppenders () {

    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
  }

  /**
   * Registers an enhancer to mutate records prior to publishing.
   *
   * @param enhancer enhancer to register
   */
  @Override
  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  /**
   * Removes all configured enhancers.
   */
  @Override
  public void clearEnhancers () {

    enhancerList.clear();
  }

  /**
   * Returns the effective log level.
   *
   * @return the scribe level equivalent of the JUL level
   */
  @Override
  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : JDKLevelTranslator.getLevel(logger.getLevel());
  }

  /**
   * Sets the level on the underlying JUL logger.
   *
   * @param level the threshold level
   */
  @Override
  public void setLevel (Level level) {

    logger.setLevel(JDKLevelTranslator.getJDKLevel(level));
  }

  /**
   * Logs a formatted message via JUL if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param message   message template
   * @param args      message arguments
   */
  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, message, args);
        ((ParameterAwareRecord<LogRecord>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  /**
   * Logs an arbitrary object via JUL if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param object    object whose {@code toString()} will be logged
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, (object == null) ? null : object.toString());
        ((ParameterAwareRecord<LogRecord>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  /**
   * Logs a lazily supplied message via JUL if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param supplier  supplier that produces the message when logging proceeds
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    JDKRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new JDKRecordSubverter(logger.getName(), level, loggerContext, throwable, (supplier == null) ? null : supplier.get());
        ((ParameterAwareRecord<LogRecord>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        logger.log(recordSubverter);
      }
    }
  }

  /**
   * Evaluates whether logging should proceed based on context and filters.
   *
   * @param level the level of the candidate record
   * @return prepared logger context if logging should continue, otherwise {@code null}
   */
  private LoggerContext willLog (Level level) {

    LoggerContext loggerContext;
    Record<LogRecord> record;

    loggerContext = new DefaultLoggerContext();
    if (getAutoFillLoggerContext()) {
      loggerContext.fillIn();
    }

    if (!((logger.getFilter() == null) && filterList.isEmpty())) {
      record = new JDKRecordSubverter(logger.getName(), level, loggerContext, null, null).getRecord();

      if (logger.getFilter() != null) {
        if (!logger.getFilter().isLoggable(record.getNativeLogEntry())) {
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

  /**
   * Runs all enhancers against the supplied record.
   *
   * @param record record to enhance
   */
  private void enhanceRecord (Record<LogRecord> record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}
