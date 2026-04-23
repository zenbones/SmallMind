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
 * Scribe {@link LoggerAdapter} that delegates to a Log4j2 {@link Logger} with additivity disabled,
 * iterating appenders directly rather than propagating events up the logger hierarchy.
 */
public class Log4JLoggerAdapter implements LoggerAdapter {

  private final Logger logger;
  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLoggerContext = false;

  /**
   * Builds an adapter around the given Log4j2 {@link Logger}, sets additivity to {@code false} to prevent
   * event propagation up the logger hierarchy, and initialises empty filter and enhancer queues.
   *
   * @param logger the Log4j2 logger that will receive all delegated log events
   */
  public Log4JLoggerAdapter (Logger logger) {

    this.logger = logger;

    logger.setAdditive(false);

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the name of the underlying Log4j2 logger.
   *
   * @return the logger name
   */
  @Override
  public String getName () {

    return logger.getName();
  }

  /**
   * Returns the shared {@link ParameterAdapter} used to supply MDC-style contextual parameters to records.
   *
   * @return the singleton {@link Parameters} instance
   */
  @Override
  public ParameterAdapter getParameterAdapter () {

    return Parameters.getInstance();
  }

  /**
   * Returns whether this adapter automatically populates caller context into the {@link LoggerContext}
   * before evaluating filters.
   *
   * @return {@code true} if auto-fill is enabled
   */
  @Override
  public boolean getAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  /**
   * Enables or disables automatic population of caller context into the {@link LoggerContext}.
   *
   * @param autoFillLoggerContext {@code true} to capture class and method name automatically
   */
  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Appends a filter to the filter chain; all registered filters must allow a record before it is published.
   *
   * @param filter the filter to add
   */
  @Override
  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  /**
   * Removes all filters from this adapter so that no filter-based veto can block publishing.
   */
  @Override
  public void clearFilters () {

    filterList.clear();
  }

  /**
   * Wraps the scribe appender in a {@link Log4JAppenderWrapper} and registers it on the underlying
   * Log4j2 logger.
   *
   * @param appender the scribe appender to add
   */
  @Override
  public void addAppender (Appender appender) {

    logger.addAppender(new Log4JAppenderWrapper(appender));
  }

  /**
   * Removes and detaches every appender currently registered on the underlying Log4j2 logger.
   */
  @Override
  public void clearAppenders () {

    for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
      logger.removeAppender(appender);
    }
  }

  /**
   * Registers an enhancer that will mutate each record after filters pass and before appenders receive it.
   *
   * @param enhancer the enhancer to add
   */
  @Override
  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  /**
   * Removes all registered enhancers so that records are published without any mutation step.
   */
  @Override
  public void clearEnhancers () {

    enhancerList.clear();
  }

  /**
   * Returns the scribe-level equivalent of the Log4j2 logger's current level, defaulting to
   * {@link Level#INFO} when the Log4j2 logger has no level set.
   *
   * @return the effective threshold level
   */
  @Override
  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : Log4JLevelTranslator.getLevel(logger.getLevel());
  }

  /**
   * Translates the given scribe level to its Log4j2 equivalent and sets it on the underlying logger.
   *
   * @param level the scribe threshold level to apply
   */
  @Override
  public void setLevel (Level level) {

    logger.setLevel(Log4JLevelTranslator.getLog4JLevel(level));
  }

  /**
   * Logs a message using a format template via Log4j2 appenders if the level meets the threshold and
   * all filters allow it; enhancers are applied before appenders receive the record.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param message   message template
   * @param args      arguments substituted into the message template
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
   * Logs the string representation of an arbitrary object via Log4j2 appenders if the level meets the
   * threshold and all filters allow it; enhancers are applied before appenders receive the record.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param object    object whose {@link Object#toString()} result is used as the message; {@code null} is tolerated
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
   * Logs a lazily evaluated message via Log4j2 appenders if the level meets the threshold and all filters
   * allow it; the supplier is not invoked when the level check fails, and enhancers are applied before
   * appenders receive the record.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param supplier  supplier invoked to produce the message string; {@code null} is tolerated
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
   * Creates and optionally fills a {@link LoggerContext}, then evaluates all scribe filters against a
   * probe record, returning the context when all pass or {@code null} when any veto.
   *
   * @param level the level of the candidate record used to build the probe
   * @return a populated {@link LoggerContext} if logging should proceed, or {@code null} if any filter vetoes it
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
   * Passes the record through every registered enhancer in insertion order.
   *
   * @param record the record to enhance before it is dispatched to Log4j2 appenders
   */
  private void enhanceRecord (Record<LogEvent> record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}
