package org.smallmind.scribe.ink.jdk;

import java.io.Serializable;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class JDKRecordFilter extends LogRecord implements RecordWrapper {

   private FilterRecord filterRecord;
   private Discriminator discriminator;
   private Level level;

   public JDKRecordFilter (Record record, Discriminator discriminator, Level level) {

      super(JDKLevelTranslator.getLog4JLevel(level), record.getMessage());

      this.discriminator = discriminator;
      this.level = level;

      filterRecord = new FilterRecord(record, this);
   }

   public String getSourceClassName () {

      if (filterRecord.getLogicalContext() != null) {
         return filterRecord.getLogicalContext().getClassName();
      }

      return null;
   }

   public String getSourceMethodName () {

      if (filterRecord.getLogicalContext() != null) {
         return filterRecord.getLogicalContext().getMethodName();
      }

      return null;
   }

   public Record getRecord () {

      return filterRecord;
   }

   private class FilterRecord implements Record {

      private Record record;
      private LogRecord logRecord;

      public FilterRecord (Record record, LogRecord logRecord) {

         this.record = record;
         this.logRecord = logRecord;
      }

      public Object getNativeLogEntry () {

         return logRecord;
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