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
package org.smallmind.scribe.ink.indigenous;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.ParameterAdapter;
import org.smallmind.scribe.pen.adapter.Parameters;

/**
 * Pure-scribe {@link LoggerAdapter} that creates {@link IndigenousRecord} instances and routes them
 * through registered filters, enhancers, and appenders without delegating to any external logging framework.
 */
public class IndigenousLoggerAdapter implements LoggerAdapter {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Appender> appenderList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private final String name;
  private Level level = Level.INFO;
  private boolean autoFillLoggerContext = false;

  /**
   * Builds an adapter for the named logger, initialising empty filter, appender, and enhancer queues
   * and defaulting the level to {@link Level#INFO}.
   *
   * @param name the logger name used to identify this adapter
   */
  public IndigenousLoggerAdapter (String name) {

    this.name = name;

    filterList = new ConcurrentLinkedQueue<>();
    appenderList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the name assigned to this logger adapter.
   *
   * @return the logger name
   */
  @Override
  public String getName () {

    return name;
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
   * Returns whether this adapter automatically populates caller context (class name, method name)
   * into the {@link LoggerContext} before evaluating filters.
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
   * Registers an appender that will receive records surviving the filter chain.
   *
   * @param appender the appender to add
   */
  @Override
  public void addAppender (Appender appender) {

    appenderList.add(appender);
  }

  /**
   * Removes all registered appenders so that no records are published until new appenders are added.
   */
  @Override
  public void clearAppenders () {

    appenderList.clear();
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
   * Returns the minimum severity level at or above which records are processed.
   *
   * @return the current threshold level
   */
  @Override
  public Level getLevel () {

    return level;
  }

  /**
   * Sets the minimum severity level; records below this threshold are silently discarded.
   *
   * @param level the new threshold level
   */
  @Override
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Logs a message using a format template if the supplied level meets the threshold and all filters allow it.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param message   message template passed to {@link MessageTranslator}
   * @param args      arguments substituted into the message template
   */
  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    IndigenousRecord record;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      record = new IndigenousRecord(name, level, throwable, message, args);
      if (willLog(record)) {
        completeLogOperation(record);
      }
    }
  }

  /**
   * Logs the string representation of an arbitrary object if the supplied level meets the threshold
   * and all filters allow the record.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param object    object whose {@link Object#toString()} result is used as the message; {@code null} is tolerated
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    IndigenousRecord record;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      record = new IndigenousRecord(name, level, throwable, (object == null) ? null : object.toString());
      if (willLog(record)) {
        completeLogOperation(record);
      }
    }
  }

  /**
   * Logs a lazily evaluated message if the supplied level meets the threshold and all filters allow the record;
   * the supplier is not invoked when the level check fails.
   *
   * @param level     severity level for the record
   * @param throwable optional throwable to associate with the record, or {@code null}
   * @param supplier  supplier invoked to produce the message string; {@code null} is tolerated
   */
  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    IndigenousRecord record;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      record = new IndigenousRecord(name, level, throwable, (supplier == null) ? null : supplier.get());
      if (willLog(record)) {
        completeLogOperation(record);
      }
    }
  }

  /**
   * Attaches a freshly created {@link LoggerContext} to the record (auto-filling it when configured)
   * and evaluates all registered filters, returning {@code false} if any filter vetoes the record.
   *
   * @param record the candidate record to evaluate
   * @return {@code true} if all filters allow the record to proceed to publishing
   */
  private boolean willLog (IndigenousRecord record) {

    LoggerContext loggerContext;

    loggerContext = new DefaultLoggerContext();
    if (getAutoFillLoggerContext()) {
      loggerContext.fillIn();
    }

    record.setLoggerContext(loggerContext);

    if (!filterList.isEmpty()) {
      for (Filter filter : filterList) {
        if (!filter.willLog(record)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Attaches current parameters to the record, runs every registered enhancer, then publishes
   * the record to each active appender.
   *
   * @param record the record to enhance and publish
   */
  private void completeLogOperation (IndigenousRecord record) {

    record.setParameters(getParameterAdapter().getParameters());

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }

    for (Appender appender : appenderList) {
      if (appender.isActive()) {
        appender.publish(record);
      }
    }
  }
}
