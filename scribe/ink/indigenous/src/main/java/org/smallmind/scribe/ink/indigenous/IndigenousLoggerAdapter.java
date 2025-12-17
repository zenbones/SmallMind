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
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.ParameterAdapter;
import org.smallmind.scribe.pen.adapter.Parameters;

/**
 * Logger adapter that emits {@link IndigenousRecord}s using indigenous appenders, filters, and enhancers.
 */
public class IndigenousLoggerAdapter implements LoggerAdapter {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Appender> appenderList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private final String name;
  private Level level = Level.INFO;
  private boolean autoFillLoggerContext = false;

  /**
   * Creates an adapter for a named logger.
   *
   * @param name the logger name
   */
  public IndigenousLoggerAdapter (String name) {

    this.name = name;

    filterList = new ConcurrentLinkedQueue<>();
    appenderList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  /**
   * Returns the logger name.
   *
   * @return the configured name
   */
  @Override
  public String getName () {

    return name;
  }

  /**
   * Supplies the parameter adapter used to manage contextual values.
   *
   * @return the shared {@link ParameterAdapter} instance
   */
  @Override
  public ParameterAdapter getParameterAdapter () {

    return Parameters.getInstance();
  }

  /**
   * Indicates whether logger context should be auto-populated prior to publishing.
   *
   * @return {@code true} when context auto-fill is enabled
   */
  @Override
  public boolean getAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  /**
   * Enables or disables automatic population of logger context data.
   *
   * @param autoFillLoggerContext {@code true} to capture context data automatically
   */
  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Adds a filter that can veto log records.
   *
   * @param filter filter to evaluate records with
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
   * Adds an appender that will receive published records.
   *
   * @param appender appender to register
   */
  @Override
  public void addAppender (Appender appender) {

    appenderList.add(appender);
  }

  /**
   * Removes all configured appenders.
   */
  @Override
  public void clearAppenders () {

    appenderList.clear();
  }

  /**
   * Adds an enhancer invoked prior to publishing.
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
   * Returns the minimum level required to log messages.
   *
   * @return the current threshold level
   */
  @Override
  public Level getLevel () {

    return level;
  }

  /**
   * Sets the minimum level required to log messages.
   *
   * @param level the threshold level
   */
  @Override
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Logs a formatted message if enabled at the supplied level.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param message   message template to format
   * @param args      substitution arguments
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
   * Logs an arbitrary object if enabled at the supplied level.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param object    object whose {@code toString()} will be logged
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
   * Logs a lazily supplied message if enabled at the supplied level.
   *
   * @param level     the level to log at
   * @param throwable optional throwable to attach
   * @param supplier  supplier that produces the message when logging proceeds
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
   * Determines whether a record should be logged after evaluating context and filters.
   *
   * @param record candidate record
   * @return {@code true} if the record should be published
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
   * Finalizes and publishes a record through enhancers and appenders.
   *
   * @param record record to publish
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
