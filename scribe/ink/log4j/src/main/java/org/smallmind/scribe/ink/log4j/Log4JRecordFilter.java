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
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class Log4JRecordFilter extends LoggingEvent implements RecordWrapper {

  private FilterRecord filterRecord;
  private AtomicReference<LocationInfo> locationInfoReference;
  private Level level;

  public Log4JRecordFilter (Record record, Level level) {

    this(record, (LoggingEvent)record.getNativeLogEntry(), level);
  }

  private Log4JRecordFilter (Record record, LoggingEvent loggingEvent, Level level) {

    super(loggingEvent.getFQNOfLoggerClass(), loggingEvent.getLogger(), loggingEvent.getTimeStamp(), Log4JLevelTranslator.getLog4JLevel(level), loggingEvent.getRenderedMessage(), loggingEvent.getThrowableInformation().getThrowable());

    this.level = level;

    filterRecord = new FilterRecord(record, this);
    locationInfoReference = new AtomicReference<LocationInfo>();
  }

  public Record getRecord () {

    return filterRecord;
  }

  public LocationInfo getLocationInformation () {

    if (locationInfoReference.get() == null) {
      synchronized (this) {
        if ((locationInfoReference.get() == null) && (filterRecord.getLogicalContext() != null)) {
          locationInfoReference.set(new LocationInfo(filterRecord.getLogicalContext().getFileName(), filterRecord.getLogicalContext().getClassName(), filterRecord.getLogicalContext().getMethodName(), String.valueOf(filterRecord.getLogicalContext().getLineNumber())));
        }
      }
    }

    return locationInfoReference.get();
  }

  private class FilterRecord implements Record {

    private Record record;
    private LoggingEvent loggingEvent;

    public FilterRecord (Record record, LoggingEvent loggingEvent) {

      this.record = record;
      this.loggingEvent = loggingEvent;
    }

    @Override
    public Object getNativeLogEntry () {

      return loggingEvent;
    }

    @Override
    public String getLoggerName () {

      return record.getLoggerName();
    }

    @Override
    public Level getLevel () {

      return level;
    }

    @Override
    public Throwable getThrown () {

      return record.getThrown();
    }

    @Override
    public String getMessage () {

      return record.getMessage();
    }

    @Override
    public Parameter[] getParameters () {

      return record.getParameters();
    }

    @Override
    public LogicalContext getLogicalContext () {

      return record.getLogicalContext();
    }

    @Override
    public long getThreadID () {

      return record.getThreadID();
    }

    @Override
    public String getThreadName () {

      return record.getThreadName();
    }

    @Override
    public long getSequenceNumber () {

      return record.getSequenceNumber();
    }

    @Override
    public long getMillis () {

      return record.getMillis();
    }
  }
}