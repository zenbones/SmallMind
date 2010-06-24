package org.smallmind.scribe.ink.log4j;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class Log4JRecordFilter extends LoggingEvent implements RecordWrapper {

   private FilterRecord filterRecord;
   private AtomicReference<LocationInfo> locationInfoReference;
   private Discriminator discriminator;
   private Level level;

   public Log4JRecordFilter (Record record, Discriminator discriminator, Level level) {

      this(record, (LoggingEvent)record.getNativeLogEntry(), discriminator, level);
   }

   private Log4JRecordFilter (Record record, LoggingEvent loggingEvent, Discriminator discriminator, Level level) {

      super(loggingEvent.getFQNOfLoggerClass(), loggingEvent.getLogger(), loggingEvent.getTimeStamp(), Log4JLevelTranslator.getLog4JLevel(level), loggingEvent.getRenderedMessage(), loggingEvent.getThrowableInformation().getThrowable());

      this.discriminator = discriminator;
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

      public Object getNativeLogEntry () {

         return loggingEvent;
      }

      public ProbeReport getProbeReport () {

         return record.getProbeReport();
      }

      public String getLoggerName () {

         return record.getLoggerName();
      }

      public Discriminator getDiscriminator () {

         return discriminator;
      }

      public Level getLevel () {

         return level;
      }

      public Throwable getThrown () {

         return record.getThrown();
      }

      public String getMessage () {

         return record.getMessage();
      }

      public void addParameter (String key, Serializable value) {

         throw new UnsupportedOperationException();
      }

      public Parameter[] getParameters () {

         return record.getParameters();
      }

      public LogicalContext getLogicalContext () {

         return record.getLogicalContext();
      }

      public long getThreadID () {

         return record.getThreadID();
      }

      public String getThreadName () {

         return record.getThreadName();
      }

      public long getSequenceNumber () {

         return record.getSequenceNumber();
      }

      public long getMillis () {

         return record.getMillis();
      }
   }
}