/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.ink.jdk;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class JDKRecordSubverter extends LogRecord implements RecordWrapper {

  private JDKRecord jdkRecord;
  private ProbeReport probeReport;
  private LogicalContext logicalContext;
  private Discriminator discriminator;
  private Level level;

  public JDKRecordSubverter (String loggerName, Discriminator discriminator, Level level, ProbeReport probeReport, LogicalContext logicalContext, Throwable throwable, String message, Object... args) {

    super(JDKLevelTranslator.getLog4JLevel(level), message);

    setLoggerName(loggerName);
    setThrown(throwable);
    setParameters(args);

    this.discriminator = discriminator;
    this.level = level;
    this.probeReport = probeReport;
    this.logicalContext = logicalContext;

    jdkRecord = new JDKRecord(this);
  }

  public String getSourceClassName () {

    if (logicalContext != null) {
      return logicalContext.getClassName();
    }

    return null;
  }

  public String getSourceMethodName () {

    if (logicalContext != null) {
      return logicalContext.getMethodName();
    }

    return null;
  }

  public Record getRecord () {

    return jdkRecord;
  }

  private class JDKRecord implements Record {

    private LogRecord logRecord;
    private HashMap<String, Serializable> parameterMap;
    private String threadName;

    public JDKRecord (LogRecord logRecord) {

      this.logRecord = logRecord;

      parameterMap = new HashMap<String, Serializable>();

      threadName = Thread.currentThread().getName();
    }

    public Object getNativeLogEntry () {

      return logRecord;
    }

    public ProbeReport getProbeReport () {

      return probeReport;
    }

    public String getLoggerName () {

      return logRecord.getLoggerName();
    }

    public Discriminator getDiscriminator () {

      return discriminator;
    }

    public Level getLevel () {

      return level;
    }

    public Throwable getThrown () {

      return logRecord.getThrown();
    }

    public String getMessage () {

      return MessageTranslator.translateMessage(logRecord.getMessage(), logRecord.getParameters());
    }

    public void addParameter (String key, Serializable value) {

      parameterMap.put(key, value);
    }

    public Parameter[] getParameters () {

      Parameter[] parameters;
      int index = 0;

      parameters = new Parameter[parameterMap.size()];
      for (Map.Entry<String, Serializable> entry : parameterMap.entrySet()) {
        parameters[index++] = new Parameter(entry.getKey(), entry.getValue());
      }

      return parameters;
    }

    public LogicalContext getLogicalContext () {

      return logicalContext;
    }

    public long getThreadID () {

      return logRecord.getThreadID();
    }

    public String getThreadName () {

      return threadName;
    }

    public long getSequenceNumber () {

      return logRecord.getSequenceNumber();

    }

    public long getMillis () {

      return logRecord.getMillis();
    }
  }
}