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