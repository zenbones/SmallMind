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
 * Abstract configuration template that declares the level, context policy, filters, appenders, and
 * enhancers to apply to every {@link Logger} it matches. Concrete subclasses implement
 * {@link #matchLogger(String)} to decide which loggers they govern; calling {@link #register()}
 * publishes the template to {@link LoggerManager} and enables live change propagation.
 */
public abstract class Template {

  /**
   * Identifies the aspect of a template that has changed and needs to be propagated to associated
   * loggers: {@code LEVEL} for the log level threshold, {@code CONTEXT} for the auto-fill context
   * flag, {@code FILTER} for the filter chain, {@code APPENDER} for the appender list, and
   * {@code ENHANCER} for the enhancer list.
   */
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
   * Constructs an empty template with a default {@link Level#INFO} threshold, context auto-fill
   * disabled, and empty filter, appender, and enhancer lists.
   */
  public Template () {

    filterList = new LinkedList<>();
    appenderList = new LinkedList<>();
    enhancerList = new LinkedList<>();
  }

  /**
   * Constructs a template with the given level threshold and context auto-fill setting, and empty
   * filter, appender, and enhancer lists.
   *
   * @param level                 default level threshold to apply to matched loggers
   * @param autoFillLoggerContext {@code true} to enable automatic context population on matched loggers
   */
  public Template (Level level, boolean autoFillLoggerContext) {

    this();

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  /**
   * Constructs a template with the given level, context auto-fill setting, and an initial set of
   * appenders; filter and enhancer lists start empty.
   *
   * @param level                 default level threshold to apply to matched loggers
   * @param autoFillLoggerContext {@code true} to enable automatic context population on matched loggers
   * @param appenders             initial appenders to attach to matched loggers; {@code null} is treated as empty
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
   * Constructs a fully configured template with explicit filters, appenders, enhancers, level, and
   * context auto-fill setting.
   *
   * @param filters               filters to evaluate on matched loggers; {@code null} is treated as empty
   * @param appenders             appenders to attach to matched loggers; {@code null} is treated as empty
   * @param enhancers             enhancers to run on records from matched loggers; {@code null} is treated as empty
   * @param level                 default level threshold to apply to matched loggers
   * @param autoFillLoggerContext {@code true} to enable automatic context population on matched loggers
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
   * Determines whether this template governs the logger with the given name and, if so, returns its
   * match priority. Higher positive values indicate a more specific (preferred) match; returning
   * {@link #NO_MATCH} means the template does not apply to this logger.
   *
   * @param loggerName fully qualified name of the logger being evaluated
   * @return non-negative priority when the logger matches, or {@link #NO_MATCH} when it does not
   */
  public abstract int matchLogger (String loggerName);

  /**
   * Registers this template with {@link LoggerManager}, immediately applying it to all currently
   * known matching loggers and enabling automatic propagation of future configuration changes.
   */
  public synchronized void register () {

    LoggerManager.addTemplate(this);
    registered = true;
  }

  /**
   * Returns the level threshold that this template applies to matched loggers.
   *
   * @return current default level; never {@code null}
   */
  public synchronized Level getLevel () {

    return level;
  }

  /**
   * Sets the level threshold applied to matched loggers and, when this template is registered,
   * immediately propagates the change to all currently associated loggers.
   *
   * @param level new level threshold; must not be {@code null}
   * @throws IllegalArgumentException if {@code level} is {@code null}
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
   * Indicates whether automatic context population is enabled for loggers matched by this template.
   *
   * @return {@code true} if context auto-fill is on; {@code false} otherwise
   */
  public synchronized boolean isAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  /**
   * Sets the context auto-fill policy and, when this template is registered, immediately propagates
   * the change to all currently associated loggers.
   *
   * @param autoFillLoggerContext {@code true} to enable automatic context population on matched loggers
   */
  public synchronized void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.CONTEXT, this);
    }
  }

  /**
   * Convenience method that replaces the entire filter list with a single filter.
   *
   * @param filter the sole filter to install; must not be {@code null}
   */
  public void setFilter (Filter filter) {

    setFilters(new Filter[] {filter});
  }

  /**
   * Returns a snapshot of the filters currently configured on this template.
   *
   * @return array of filters in their configured order; never {@code null}
   */
  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  /**
   * Replaces the entire filter list with the supplied array and, when registered, propagates the
   * change to all associated loggers.
   *
   * @param filters new set of filters to install; must not be {@code null}
   */
  public synchronized void setFilters (Filter[] filters) {

    filterList.clear();
    filterList.addAll(Arrays.asList(filters));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Appends a filter to the filter list and, when registered, propagates the change to all
   * associated loggers.
   *
   * @param filter filter to add; must not be {@code null}
   */
  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Removes a filter from the filter list and, if the removal changed the list and this template is
   * registered, propagates the change to all associated loggers.
   *
   * @param filter filter to remove; no-op if the filter is not present
   */
  public synchronized void removeFilter (Filter filter) {

    if (filterList.remove(filter) && registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  /**
   * Convenience method that replaces the entire appender list with a single appender.
   *
   * @param appender the sole appender to install; must not be {@code null}
   */
  public void setAppender (Appender appender) {

    setAppenders(new Appender[] {appender});
  }

  /**
   * Returns a snapshot of the appenders currently configured on this template.
   *
   * @return array of appenders in their configured order; never {@code null}
   */
  public synchronized Appender[] getAppenders () {

    Appender[] appenders;

    appenders = new Appender[appenderList.size()];
    appenderList.toArray(appenders);

    return appenders;
  }

  /**
   * Replaces the entire appender list with the supplied array and, when registered, propagates the
   * change to all associated loggers.
   *
   * @param appenders new set of appenders to install; must not be {@code null}
   */
  public synchronized void setAppenders (Appender[] appenders) {

    appenderList.clear();
    appenderList.addAll(Arrays.asList(appenders));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Appends an appender to the appender list and, when registered, propagates the change to all
   * associated loggers.
   *
   * @param appender appender to add; must not be {@code null}
   */
  public synchronized void addAppender (Appender appender) {

    appenderList.add(appender);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Removes an appender from the appender list and, if the removal changed the list and this
   * template is registered, propagates the change to all associated loggers.
   *
   * @param appender appender to remove; no-op if the appender is not present
   */
  public synchronized void removeAppender (Appender appender) {

    if (appenderList.remove(appender) && registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  /**
   * Convenience method that replaces the entire enhancer list with a single enhancer.
   *
   * @param enhancer the sole enhancer to install; must not be {@code null}
   */
  public void setEnhancer (Enhancer enhancer) {

    setEnhancers(new Enhancer[] {enhancer});
  }

  /**
   * Returns a snapshot of the enhancers currently configured on this template.
   *
   * @return array of enhancers in their configured order; never {@code null}
   */
  public synchronized Enhancer[] getEnhancers () {

    Enhancer[] enhancers;

    enhancers = new Enhancer[enhancerList.size()];
    enhancerList.toArray(enhancers);

    return enhancers;
  }

  /**
   * Replaces the entire enhancer list with the supplied array and, when registered, propagates the
   * change to all associated loggers.
   *
   * @param enhancers new set of enhancers to install; must not be {@code null}
   */
  public synchronized void setEnhancers (Enhancer[] enhancers) {

    enhancerList.clear();
    enhancerList.addAll(Arrays.asList(enhancers));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Appends an enhancer to the enhancer list and, when registered, propagates the change to all
   * associated loggers.
   *
   * @param enhancer enhancer to add; must not be {@code null}
   */
  public synchronized void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Removes an enhancer from the enhancer list and, if the removal changed the list and this
   * template is registered, propagates the change to all associated loggers.
   *
   * @param enhancer enhancer to remove; no-op if the enhancer is not present
   */
  public synchronized void removeEnhancer (Enhancer enhancer) {

    if (enhancerList.remove(enhancer) && registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  /**
   * Applies every configuration aspect of this template to the given logger by iterating over all
   * {@link Change} values and calling {@link #applyChange(Change, Logger)}.
   *
   * @param logger the logger to configure with all settings from this template
   */
  protected synchronized void apply (Logger logger) {

    for (Change change : Change.values()) {
      applyChange(change, logger);
    }
  }

  /**
   * Applies a single configuration aspect to the given logger, pushing the current template value
   * for that aspect (level, context flag, filter list, appender list, or enhancer list) onto the
   * logger.
   *
   * @param change the aspect of this template to apply
   * @param logger the logger to update
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
