/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class Log4JRecordSubverter extends LoggingEvent implements RecordWrapper {

  private static final Parameter[] NO_PARAMETERS = new Parameter[0];

  private Log4JRecord log4jRecord;
  private ProbeReport probeReport;
  private LogicalContext logicalContext;
  private AtomicReference<LocationInfo> locationInfoReference;
  private Discriminator discriminator;
  private Level level;

  public Log4JRecordSubverter (Logger logger, Discriminator discriminator, Level level, ProbeReport probeReport, LogicalContext logicalContext, Throwable throwable, String message, Object... args) {

    super(logger.getClass().getCanonicalName(), logger, System.currentTimeMillis(), Log4JLevelTranslator.getLog4JLevel(level), MessageTranslator.translateMessage(message, args), throwable);

    this.discriminator = discriminator;
    this.level = level;
    this.probeReport = probeReport;
    this.logicalContext = logicalContext;

    log4jRecord = new Log4JRecord(this);

    locationInfoReference = new AtomicReference<LocationInfo>();
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

  private class Log4JRecord implements Record {

    private LoggingEvent loggingEvent;
    private HashMap<String, Serializable> parameterMap;
    private long threadId;
    private long sequenceNumber;

    public Log4JRecord (LoggingEvent loggingEvent) {

      this.loggingEvent = loggingEvent;

      parameterMap = new HashMap<String, Serializable>();

      threadId = Thread.currentThread().getId();
      sequenceNumber = SequenceGenerator.next();
    }

    public Object getNativeLogEntry () {

      return loggingEvent;
    }

    public ProbeReport getProbeReport () {

      return probeReport;
    }

    public String getLoggerName () {

      return loggingEvent.getLoggerName();
    }

    public Discriminator getDiscriminator () {

      return discriminator;
    }

    public Level getLevel () {

      return level;
    }

    public Throwable getThrown () {

      return (loggingEvent.getThrowableInformation() != null) ? loggingEvent.getThrowableInformation().getThrowable() : null;
    }

    public String getMessage () {

      return loggingEvent.getRenderedMessage();
    }

    public void addParameter (String key, Serializable value) {

      parameterMap.put(key, value);
    }

    public Parameter[] getParameters () {

      if (parameterMap.isEmpty()) {

        return NO_PARAMETERS;
      }
      else {

        Parameter[] parameters;
        int index = 0;

        parameters = new Parameter[parameterMap.size()];
        for (Map.Entry<String, Serializable> entry : parameterMap.entrySet()) {
          parameters[index++] = new Parameter(entry.getKey(), entry.getValue());
        }

        return parameters;
      }
    }

    public LogicalContext getLogicalContext () {

      return logicalContext;
    }

    public long getThreadID () {

      return threadId;
    }

    public String getThreadName () {

      return loggingEvent.getThreadName();
    }

    public long getSequenceNumber () {

      return sequenceNumber;
    }

    public long getMillis () {

      return loggingEvent.getTimeStamp();
    }
  }
}