/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class IndigenousLoggerAdapter implements LoggerAdapter {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Appender> appenderList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;
  private final String name;
  private Level level = Level.INFO;
  private boolean autoFillLoggerContext = false;

  public IndigenousLoggerAdapter (String name) {

    this.name = name;

    filterList = new ConcurrentLinkedQueue<>();
    appenderList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  @Override
  public String getName () {

    return name;
  }

  @Override
  public ParameterAdapter getParameterAdapter () {

    return Parameters.getInstance();
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
  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  @Override
  public void clearFilters () {

    filterList.clear();
  }

  @Override
  public void addAppender (Appender appender) {

    appenderList.add(appender);
  }

  @Override
  public void clearAppenders () {

    appenderList.clear();
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

    return level;
  }

  @Override
  public void setLevel (Level level) {

    this.level = level;
  }

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