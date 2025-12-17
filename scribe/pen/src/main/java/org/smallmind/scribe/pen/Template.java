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
package org.smallmind.scribe.pen;

import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Configurable template describing how loggers should behave (level, context, filters, appenders, enhancers).
 * Implementations decide which loggers they match and can be registered with {@link LoggerManager}.
 */
public abstract class Template {

  public enum Change {

    LEVEL, CONTEXT, FILTER, APPENDER, ENHANCER
  }

  public final static int NO_MATCH = -1;

  private final LinkedList<Filter> filterList;
  private final LinkedList<Appender> appenderList;
  private final LinkedList<Enhancer> enhancerList;
  private Level level = Level.INFO;
  private boolean autoFillLoggerContext = false;
  private boolean registered = false;

  /**
   * Constructs an empty template with default INFO level and no context auto-fill.
   */
  public Template () {

    filterList = new LinkedList<>();
    appenderList = new LinkedList<>();
    enhancerList = new LinkedList<>();
  }

  /**
   * Constructs a template with level and context auto-fill options.
   *
   * @param level                 default level
   * @param autoFillLoggerContext {@code true} to automatically capture context
   */
  public Template (Level level, boolean autoFillLoggerContext) {

    this();

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Constructs a template with level, context auto-fill, and initial appenders.
   *
   * @param level                 default level
   * @param autoFillLoggerContext {@code true} to automatically capture context
   * @param appenders             appenders to attach
   */
  public Template (Level level, boolean autoFillLoggerContext, Appender... appenders) {

    this();

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;

    if (appenders != null) {
      appenderList.addAll(Arrays.asList(appenders));
    }
  }

  /**
   * Constructs a template with full component lists and level/context options.
   *
   * @param filters               filters to apply
   * @param appenders             appenders to attach
   * @param enhancers             enhancers to run
   * @param level                 default level
   * @param autoFillLoggerContext {@code true} to automatically capture context
   */
  public Template (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext) {

    this();

    if (filters != null) {
      filterList.addAll(Arrays.asList(filters));
    }
    if (appenders != null) {
      appenderList.addAll(Arrays.asList(appenders));
    }
    if (enhancers != null) {
      enhancerList.addAll(Arrays.asList(enhancers));
    }

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Calculates a match score for a logger name. Higher values indicate better matches; {@link #NO_MATCH} means no match.
   *
   * @param loggerName logger name under consideration
   * @return integer priority or {@link #NO_MATCH}
   */
  public abstract int matchLogger (String loggerName);

  /**
   * Registers this template with {@link LoggerManager}, applying it to matching loggers and enabling change propagation.
   */
  public synchronized void register () {

    LoggerManager.addTemplate(this);
    registered = true;
  }

  /**
   * Returns the default level for loggers matched by this template.
   *
   * @return default level
   */
  public synchronized Level getLevel () {

    return level;
  }

  /**
   * Sets the default level for loggers matched by this template and propagates the change when registered.
   *
   * @param level default level; must not be {@code null}
   * @throws IllegalArgumentException if level is {@code null}
   */
  public synchronized void setLevel (Level level) {

    if (level == null) {
      throw new IllegalArgumentException("Can't set a 'null' default level");
    }

    this.level = level;

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.LEVEL, this);
    }
  }

  /**
   * Indicates whether logger context should be auto-filled for matched loggers.
   *
   * @return {@code true} when context auto-fill is enabled
   */
  public synchronized boolean isAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  /**
   * Enables or disables automatic context population for matched loggers, propagating the change when registered.
   *
   * @param autoFillLoggerContext {@code true} to capture context data automatically
   */
  public synchronized void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.CONTEXT, this);
    }
  }

  /**
   * Convenience setter for a single filter.
   *
   * @param filter filter to install
   */
  public void setFilter (Filter filter) {

    setFilters(new Filter[] {filter});
  }

  /**
   * Returns the configured filters.
   *
   * @return array of filters
   */
  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  /**
   * Replaces all filters and propagates the change when registered.
   *
   * @param filters filters to install
   */
  public synchronized void setFilters (Filter[] filters) {

    filterList.clear();
    filterList.addAll(Arrays.asList(filters));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Adds a single filter and propagates the change when registered.
   *
   * @param filter filter to add
   */
  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Removes a filter and propagates the change when registered.
   *
   * @param filter filter to remove
   */
  public synchronized void removeFilter (Filter filter) {

    if (filterList.remove(filter) && registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Convenience setter for a single appender.
   *
   * @param appender appender to install
   */
  public void setAppender (Appender appender) {

    setAppenders(new Appender[] {appender});
  }

  /**
   * Returns the configured appenders.
   *
   * @return array of appenders
   */
  public synchronized Appender[] getAppenders () {

    Appender[] appenders;

    appenders = new Appender[appenderList.size()];
    appenderList.toArray(appenders);

    return appenders;
  }

  /**
   * Replaces all appenders and propagates the change when registered.
   *
   * @param appenders appenders to install
   */
  public synchronized void setAppenders (Appender[] appenders) {

    appenderList.clear();
    appenderList.addAll(Arrays.asList(appenders));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Adds an appender and propagates the change when registered.
   *
   * @param appender appender to add
   */
  public synchronized void addAppender (Appender appender) {

    appenderList.add(appender);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Removes an appender and propagates the change when registered.
   *
   * @param appender appender to remove
   */
  public synchronized void removeAppender (Appender appender) {

    if (appenderList.remove(appender) && registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Convenience setter for a single enhancer.
   *
   * @param enhancer enhancer to install
   */
  public void setEnhancer (Enhancer enhancer) {

    setEnhancers(new Enhancer[] {enhancer});
  }

  /**
   * Returns the configured enhancers.
   *
   * @return array of enhancers
   */
  public synchronized Enhancer[] getEnhancers () {

    Enhancer[] enhancers;

    enhancers = new Enhancer[enhancerList.size()];
    enhancerList.toArray(enhancers);

    return enhancers;
  }

  /**
   * Replaces all enhancers and propagates the change when registered.
   *
   * @param enhancers enhancers to install
   */
  public synchronized void setEnhancers (Enhancer[] enhancers) {

    enhancerList.clear();
    enhancerList.addAll(Arrays.asList(enhancers));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Adds an enhancer and propagates the change when registered.
   *
   * @param enhancer enhancer to add
   */
  public synchronized void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Removes an enhancer and propagates the change when registered.
   *
   * @param enhancer enhancer to remove
   */
  public synchronized void removeEnhancer (Enhancer enhancer) {

    if (enhancerList.remove(enhancer) && registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Applies all template settings to the provided logger.
   *
   * @param logger logger to configure
   */
  protected synchronized void apply (Logger logger) {

    for (Change change : Change.values()) {
      applyChange(change, logger);
    }
  }

  /**
   * Applies a single template change to the provided logger.
   *
   * @param change change to apply
   * @param logger logger to configure
   */
  protected synchronized void applyChange (Change change, Logger logger) {

    switch (change) {

      case LEVEL:
        logger.setLevel(level);
        break;
      case CONTEXT:
        logger.setAutoFillLoggerContext(autoFillLoggerContext);
        break;
      case FILTER:
        logger.clearFilters();
        for (Filter filter : filterList) {
          logger.addFilter(filter);
        }
        break;
      case APPENDER:
        logger.clearAppenders();
        for (Appender appender : appenderList) {
          logger.addAppender(appender);
        }
        break;
      case ENHANCER:
        logger.clearEnhancers();
        for (Enhancer enhancer : enhancerList) {
          logger.addEnhancer(enhancer);
        }
        break;
      default:
        throw new UnknownSwitchCaseException(change.name());
    }
  }
}
