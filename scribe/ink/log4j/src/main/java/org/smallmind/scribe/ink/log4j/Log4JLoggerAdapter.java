/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.apache.log4j.Logger;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class Log4JLoggerAdapter implements LoggerAdapter {

  private Logger logger;
  private ConcurrentLinkedQueue<Filter> filterList;
  private ConcurrentLinkedQueue<Enhancer> enhancerList;
  private boolean autoFillLogicalContext = false;

  public Log4JLoggerAdapter (Logger logger) {

    this.logger = logger;

    logger.setAdditivity(false);

    filterList = new ConcurrentLinkedQueue<Filter>();
    enhancerList = new ConcurrentLinkedQueue<Enhancer>();
  }

  public String getName () {

    return logger.getName();
  }

  public boolean getAutoFillLogicalContext () {

    return autoFillLogicalContext;
  }

  public void setAutoFillLogicalContext (boolean autoFillLogicalContext) {

    this.autoFillLogicalContext = autoFillLogicalContext;
  }

  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  public void clearFilters () {

    filterList.clear();
  }

  public void addAppender (Appender appender) {

    logger.addAppender(new Log4JAppenderWrapper(appender));
  }

  public Appender removeAppender (Appender appender) {

    return removeAppender(appender.getName());
  }

  public Appender removeAppender (String name) {

    org.apache.log4j.Appender appender;

    if ((appender = logger.getAppender(name)) != null) {
      logger.removeAppender(name);
      if (appender instanceof Log4JAppenderWrapper) {
        return ((Log4JAppenderWrapper)appender).getInnerAppender();
      }
      else {
        throw new UnsupportedOperationException("Appender can't be returned via this interface because it's Log4J native");
      }
    }

    return null;
  }

  public void clearAppenders () {

    logger.removeAllAppenders();
  }

  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  public void clearEnhancers () {

    enhancerList.clear();
  }

  public Level getLevel () {

    return (logger.getLevel() == null) ? Level.INFO : Log4JLevelTranslator.getLevel(logger.getLevel());
  }

  public void setLevel (Level level) {

    logger.setLevel(Log4JLevelTranslator.getLog4JLevel(level));
  }

  public void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args) {

    Log4JRecordSubverter recordSubverter;
    LogicalContext logicalContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((logicalContext = willLog(discriminator, level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger, discriminator, level, null, logicalContext, throwable, message, args);
        enhanceRecord(recordSubverter.getRecord());
        logger.callAppenders(recordSubverter);
      }
    }
  }

  public void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport) {

    Log4JRecordSubverter recordSubverter;
    LogicalContext logicalContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((logicalContext = willLog(discriminator, level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger, discriminator, level, probeReport, logicalContext, throwable, (probeReport.getTitle() == null) ? "Probe Report" : probeReport.getTitle());
        enhanceRecord(recordSubverter.getRecord());
        logger.callAppenders(recordSubverter);
      }
    }
  }

  public void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object) {

    Log4JRecordSubverter recordSubverter;
    LogicalContext logicalContext;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if ((logicalContext = willLog(discriminator, level)) != null) {
        recordSubverter = new Log4JRecordSubverter(logger, discriminator, level, null, logicalContext, throwable, (object == null) ? null : object.toString());
        enhanceRecord(recordSubverter.getRecord());
        logger.callAppenders(recordSubverter);
      }
    }
  }

  private LogicalContext willLog (Discriminator discriminator, Level level) {

    LogicalContext logicalContext;
    Record filterRecord;

    logicalContext = new DefaultLogicalContext();
    if (getAutoFillLogicalContext()) {
      logicalContext.fillIn();
    }

    if (!filterList.isEmpty()) {
      filterRecord = new Log4JRecordSubverter(logger, discriminator, level, null, logicalContext, null, null).getRecord();
      for (Filter filter : filterList) {
        if (!filter.willLog(filterRecord)) {
          return null;
        }
      }
    }

    return logicalContext;
  }

  private void enhanceRecord (Record record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}