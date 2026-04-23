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
 * Scribe {@link LoggerAdapter} that delegates to a JUL {@link Logger}, wrapping scribe appenders as JUL
 * {@link Handler}s, scribe filters as JUL filters, and translating levels through {@link JDKLevelTranslator}.
 */
public class JDKLoggerAdapter implements LoggerAdapter {

  private final Logger logger;

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLoggerContext = false;

  /**
   * Builds an adapter around the given JUL {@link Logger}, initialising empty filter and enhancer queues.
   *
   * @param logger the JUL logger that will receive all delegated log events
   */
  public JDKLoggerAdapter (Logger logger) {

    this.logger = logger;

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the name of the underlying JUL logger.
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
   * Adds a scribe filter to the local filter list and installs a corresponding {@link JDKFilterWrapper}
   * on the underlying JUL logger; both operations are performed atomically.
   *
   * @param filter the scribe filter to add
   */
  @Override
  public synchronized void addFilter (Filter filter) {

    synchronized (logger) {
      filterList.add(filter);
      logger.setFilter(new JDKFilterWrapper(filter));
    }
  }

  /**
   * Removes all scribe filters and clears the JUL logger's filter so that no filter-based veto
   * can block publishing.
   */
  @Override
  public synchronized void clearFilters () {

    filterList.clear();
    logger.setFilter(null);
  }

  /**
   * Wraps the scribe appender in a {@link JDKAppenderWrapper} and registers it as a JUL handler.
   *
   * @param appender the scribe appender to add
   */
  @Override
  public synchronized void addAppender (Appender appender) {

    logger.addHandler(new JDKAppenderWrapper(appender));
  }

  /**
   * Removes and detaches every handler currently registered on the underlying JUL logger.
   */
  @Override
  public synchronized void clearAppenders () {

    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
  }

  /**
   * Registers an enhancer that will mutate each record after filters pass and before handlers receive it.
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
   * Returns the scribe-level equivalent of the JUL logger's current level, defaulting to
   * {@link Level#INFO} when the JUL logger has no level set.
   *
   * @return the effective threshold level
   */
  @Override
  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : JDKLevelTranslator.getLevel(logger.getLevel());
  }

  /**
   * Translates the given scribe level to its JUL equivalent and sets it on the underlying JUL logger.
   *
   * @param level the scribe threshold level to apply
   */
  @Override
  public void setLevel (Level level) {

    logger.setLevel(JDKLevelTranslator.getJDKLevel(level));
  }

  /**
   * Logs a message using a format template via JUL if the level meets the threshold and all filters allow it;
   * enhancers are applied before the record is handed to JUL.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param message   message template
   * @param args      arguments substituted into the message template
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
   * Logs the string representation of an arbitrary object via JUL if the level meets the threshold
   * and all filters allow the record; enhancers are applied before the record is handed to JUL.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param object    object whose {@link Object#toString()} result is used as the message; {@code null} is tolerated
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
   * Logs a lazily evaluated message via JUL if the level meets the threshold and all filters allow the record;
   * the supplier is not invoked when the level check fails, and enhancers are applied before the record
   * is handed to JUL.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param supplier  supplier invoked to produce the message string; {@code null} is tolerated
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
   * Creates and optionally fills a {@link LoggerContext}, then evaluates the JUL logger's filter and
   * all scribe filters against a probe record, returning the context when all pass or {@code null}
   * when any veto.
   *
   * @param level the level of the candidate record used to build the probe
   * @return a populated {@link LoggerContext} if logging should proceed, or {@code null} if any filter vetoes it
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
   * Passes the record through every registered enhancer in insertion order.
   *
   * @param record the record to enhance before it is dispatched to JUL handlers
   */
  private void enhanceRecord (Record<LogRecord> record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}
