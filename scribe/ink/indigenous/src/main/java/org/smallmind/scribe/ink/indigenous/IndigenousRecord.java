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
package org.smallmind.scribe.ink.indigenous;

import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class IndigenousRecord extends ParameterAwareRecord implements RecordWrapper {

  private LogicalContext logicalContext;
  private Level level;
  private Throwable throwable;
  private String loggerName;
  private String message;
  private String threadName;
  private Object[] args;
  private long millis;
  private long threadId;
  private long sequenceNumber;

  public IndigenousRecord (String loggerName, Level level, Throwable throwable, String message, Object... args) {

    this.loggerName = loggerName;
    this.level = level;
    this.throwable = throwable;
    this.message = message;
    this.args = args;

    millis = System.currentTimeMillis();

    threadId = Thread.currentThread().getId();
    threadName = Thread.currentThread().getName();
    sequenceNumber = SequenceGenerator.next();
  }

  @Override
  public Record getRecord () {

    return this;
  }

  @Override
  public Object getNativeLogEntry () {

    return this;
  }

  @Override
  public String getLoggerName () {

    return loggerName;
  }

  @Override
  public Level getLevel () {

    return level;
  }

  @Override
  public Throwable getThrown () {

    return throwable;
  }

  @Override
  public String getMessage () {

    return MessageTranslator.translateMessage(message, args);
  }

  @Override
  public LogicalContext getLogicalContext () {

    return logicalContext;
  }

  public void setLogicalContext (LogicalContext logicalContext) {

    this.logicalContext = logicalContext;
  }

  @Override
  public long getThreadID () {

    return threadId;
  }

  @Override
  public String getThreadName () {

    return threadName;
  }

  @Override
  public long getSequenceNumber () {

    return sequenceNumber;
  }

  @Override
  public long getMillis () {

    return millis;
  }
}
