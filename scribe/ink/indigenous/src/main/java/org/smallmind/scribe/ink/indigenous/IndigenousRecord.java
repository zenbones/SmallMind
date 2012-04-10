/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.scribe.ink.indigenous;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class IndigenousRecord implements Record, RecordWrapper {

   private ProbeReport probeReport;
   private LogicalContext logicalContext;
   private Discriminator discriminator;
   private Level level;
   private Throwable throwable;
   private HashMap<String, Serializable> parameterMap;
   private String loggerName;
   private String message;
   private String threadName;
   private Object[] args;
   private long millis;
   private long threadId;
   private long sequenceNumber;

   public IndigenousRecord (String loggerName, Discriminator discriminator, Level level, ProbeReport probeReport, Throwable throwable, String message, Object... args) {

      this.loggerName = loggerName;
      this.discriminator = discriminator;
      this.level = level;
      this.probeReport = probeReport;
      this.throwable = throwable;
      this.message = message;
      this.args = args;

      millis = System.currentTimeMillis();

      parameterMap = new HashMap<String, Serializable>();

      threadId = Thread.currentThread().getId();
      threadName = Thread.currentThread().getName();
      sequenceNumber = SequenceGenerator.next();
   }

   public void setLogicalContext (LogicalContext logicalContext) {

      this.logicalContext = logicalContext;
   }

   public Record getRecord () {

      return this;
   }

   public Object getNativeLogEntry () {

      return this;
   }

   public ProbeReport getProbeReport () {

      return probeReport;
   }

   public String getLoggerName () {

      return loggerName;
   }

   public Discriminator getDiscriminator () {

      return discriminator;
   }

   public Level getLevel () {

      return level;
   }

   public Throwable getThrown () {

      return throwable;
   }

   public String getMessage () {

      return MessageTranslator.translateMessage(message, args);
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

      return threadId;
   }

   public String getThreadName () {

      return threadName;
   }

   public long getSequenceNumber () {

      return sequenceNumber;
   }

   public long getMillis () {

      return millis;
   }
}
