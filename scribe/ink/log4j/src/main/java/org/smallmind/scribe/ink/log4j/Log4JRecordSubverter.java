/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class Log4JRecordSubverter extends LoggingEvent implements RecordWrapper {

  private static final Parameter[] NO_PARAMETERS = new Parameter[0];

  private Log4JRecord log4jRecord;
  private LogicalContext logicalContext;
  private AtomicReference<LocationInfo> locationInfoReference;
  private Level level;

  public Log4JRecordSubverter (Logger logger, Level level, LogicalContext logicalContext, Throwable throwable, String message, Object... args) {

    super(logger.getClass().getCanonicalName(), logger, System.currentTimeMillis(), Log4JLevelTranslator.getLog4JLevel(level), MessageTranslator.translateMessage(message, args), throwable);

    this.level = level;
    this.logicalContext = logicalContext;

    log4jRecord = new Log4JRecord(this);

    locationInfoReference = new AtomicReference<>();
  }

  public Record getRecord () {

    return log4jRecord;
  }

  public LocationInfo getLocationInformation () {

    if (locationInfoReference.get() == null) {
      synchronized (this) {
        if ((locationInfoReference.get() == null) && (logicalContext != null)) {
          locationInfoReference.set(new LocationInfo(logicalContext.getFileName(), logicalContext.getClassName(), logicalContext.getMethodName(), String.valueOf(logicalContext.getLineNumber())));
        }
      }
    }

    return locationInfoReference.get();
  }

  private class Log4JRecord extends ParameterAwareRecord {

    private LoggingEvent loggingEvent;
    private long threadId;
    private long sequenceNumber;

    public Log4JRecord (LoggingEvent loggingEvent) {

      this.loggingEvent = loggingEvent;

      threadId = Thread.currentThread().getId();
      sequenceNumber = SequenceGenerator.next();
    }

    @Override
    public Object getNativeLogEntry () {

      return loggingEvent;
    }

    @Override
    public String getLoggerName () {

      return loggingEvent.getLoggerName();
    }

    @Override
    public Level getLevel () {

      return level;
    }

    @Override
    public Throwable getThrown () {

      return (loggingEvent.getThrowableInformation() != null) ? loggingEvent.getThrowableInformation().getThrowable() : null;
    }

    @Override
    public String getMessage () {

      return loggingEvent.getRenderedMessage();
    }

    @Override
    public LogicalContext getLogicalContext () {

      return logicalContext;
    }

    @Override
    public long getThreadID () {

      return threadId;
    }

    @Override
    public String getThreadName () {

      return loggingEvent.getThreadName();
    }

    @Override
    public long getSequenceNumber () {

      return sequenceNumber;
    }

    @Override
    public long getMillis () {

      return loggingEvent.getTimeStamp();
    }
  }
}