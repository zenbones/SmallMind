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
