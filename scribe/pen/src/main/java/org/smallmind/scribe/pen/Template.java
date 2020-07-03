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

import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

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

  public Template () {

    filterList = new LinkedList<>();
    appenderList = new LinkedList<>();
    enhancerList = new LinkedList<>();
  }

  public Template (Level level, boolean autoFillLoggerContext) {

    this();

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  public Template (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLoggerContext) {

    this();

    filterList.addAll(Arrays.asList(filters));
    appenderList.addAll(Arrays.asList(appenders));
    enhancerList.addAll(Arrays.asList(enhancers));

    this.level = level;
    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  public abstract int matchLogger (String loggerName);

  public synchronized void register () {

    LoggerManager.addTemplate(this);
    registered = true;
  }

  public synchronized Level getLevel () {

    return level;
  }

  public synchronized void setLevel (Level level) {

    if (level == null) {
      throw new IllegalArgumentException("Can't set a 'null' default level");
    }

    this.level = level;

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.LEVEL, this);
    }
  }

  public synchronized boolean isautoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  public synchronized void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.CONTEXT, this);
    }
  }

  public void setFilter (Filter filter) {

    setFilters(new Filter[] {filter});
  }

  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  public synchronized void setFilters (Filter[] filters) {

    filterList.clear();
    filterList.addAll(Arrays.asList(filters));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  public synchronized void removeFilter (Filter filter) {

    if (filterList.remove(filter) && registered) {
      LoggerManager.commitTemplateChanges(Change.FILTER, this);
    }
  }

  public void setAppender (Appender appender) {

    setAppenders(new Appender[] {appender});
  }

  public synchronized Appender[] getAppenders () {

    Appender[] appenders;

    appenders = new Appender[appenderList.size()];
    appenderList.toArray(appenders);

    return appenders;
  }

  public synchronized void setAppenders (Appender[] appenders) {

    appenderList.clear();
    appenderList.addAll(Arrays.asList(appenders));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  public synchronized void addAppender (Appender appender) {

    appenderList.add(appender);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  public synchronized void removeAppender (Appender appender) {

    if (appenderList.remove(appender) && registered) {
      LoggerManager.commitTemplateChanges(Change.APPENDER, this);
    }
  }

  public void setEnhancer (Enhancer enhancer) {

    setEnhancers(new Enhancer[] {enhancer});
  }

  public synchronized Enhancer[] getEnhancers () {

    Enhancer[] enhancers;

    enhancers = new Enhancer[enhancerList.size()];
    enhancerList.toArray(enhancers);

    return enhancers;
  }

  public synchronized void setEnhancers (Enhancer[] enhancers) {

    enhancerList.clear();
    enhancerList.addAll(Arrays.asList(enhancers));

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  public synchronized void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);

    if (registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  public synchronized void removeEnhancer (Enhancer enhancer) {

    if (enhancerList.remove(enhancer) && registered) {
      LoggerManager.commitTemplateChanges(Change.ENHANCER, this);
    }
  }

  protected synchronized void apply (Logger logger) {

    for (Change change : Change.values()) {
      applyChange(change, logger);
    }
  }

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
