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
package org.smallmind.scribe.ink.log4j;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
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
 * Logger adapter that routes scribe logging calls to Log4j2.
 */
public class Log4JLoggerAdapter implements LoggerAdapter {

  private final Logger logger;
  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLoggerContext = false;

  /**
   * Creates an adapter around a Log4j2 {@link Logger}.
   *
   * @param logger the Log4j2 logger to delegate to
   */
  public Log4JLoggerAdapter (Logger logger) {

    this.logger = logger;

    logger.setAdditive(false);

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the logger name.
   *
   * @return the logger name
   */
  @Override
  public String getName () {

    return logger.getName();
  }

  /**
   * Supplies the parameter adapter used to store contextual values.
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
   * Registers a filter that can veto log records.
   *
   * @param filter filter to add
   */
  @Override
  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  /**
   * Removes all configured filters.
   */
  @Override
  public void clearFilters () {

    filterList.clear();
  }

  /**
   * Adds an appender by wrapping it in a Log4j2 appender wrapper.
   *
   * @param appender appender to register
   */
  @Override
  public void addAppender (Appender appender) {

    logger.addAppender(new Log4JAppenderWrapper(appender));
  }

  /**
   * Removes all appenders currently attached to the Log4j2 logger.
   */
  @Override
  public void clearAppenders () {

    for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
      logger.removeAppender(appender);
    }
  }

  /**
   * Registers an enhancer to mutate records prior to publishing.
   *
   * @param enhancer enhancer to add
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
   * @return the scribe level equivalent of the Log4j2 level
   */
  @Override
  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : Log4JLevelTranslator.getLevel(logger.getLevel());
  }

  /**
   * Sets the level on the underlying Log4j2 logger.
   *
   * @param level the threshold level
   */
  @Override
  public void setLevel (Level level) {

    logger.setLevel(Log4JLevelTranslator.getLog4JLevel(level));
  }

  /**
   * Logs a formatted message via Log4j2 if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param message   message template
   * @param args      message arguments
   */
  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    Log4JRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger.getName(), logger.getClass().getCanonicalName(), level, loggerContext, throwable, message, args);
        ((ParameterAwareRecord<LogEvent>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
          appender.append(recordSubverter);
        }
      }
    }
  }

  /**
   * Logs an arbitrary object via Log4j2 if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param object    object whose {@code toString()} will be logged
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    Log4JRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger.getName(), logger.getClass().getCanonicalName(), level, loggerContext, throwable, (object == null) ? null : object.toString());
        ((ParameterAwareRecord<LogEvent>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
          appender.append(recordSubverter);
        }
      }
    }
  }

  /**
   * Logs a lazily supplied message via Log4j2 if permitted by level and filters.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param supplier  supplier that produces the message when logging proceeds
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    Log4JRecordSubverter recordSubverter;
    LoggerContext loggerContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((loggerContext = willLog(level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger.getName(), logger.getClass().getCanonicalName(), level, loggerContext, throwable, (supplier == null) ? null : supplier.get());
        ((ParameterAwareRecord<LogEvent>)recordSubverter.getRecord()).setParameters(getParameterAdapter().getParameters());
        enhanceRecord(recordSubverter.getRecord());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
          appender.append(recordSubverter);
        }
      }
    }
  }

  /**
   * Evaluates whether logging should proceed based on filters and context.
   *
   * @param level the level of the candidate record
   * @return prepared logger context if logging should continue, otherwise {@code null}
   */
  private LoggerContext willLog (Level level) {

    LoggerContext loggerContext;
    Record<LogEvent> filterRecord;

    loggerContext = new DefaultLoggerContext();
    if (getAutoFillLoggerContext()) {
      loggerContext.fillIn();
    }

    if (!filterList.isEmpty()) {
      filterRecord = new Log4JRecordSubverter(logger.getName(), logger.getClass().getCanonicalName(), level, loggerContext, null, null).getRecord();
      for (Filter filter : filterList) {
        if (!filter.willLog(filterRecord)) {
          return null;
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
  private void enhanceRecord (Record<LogEvent> record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}
