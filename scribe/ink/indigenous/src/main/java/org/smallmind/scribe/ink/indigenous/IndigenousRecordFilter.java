package org.smallmind.scribe.ink.indigenous;

import java.io.Serializable;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class IndigenousRecordFilter implements Record, RecordWrapper {

   private Record record;
   private Discriminator discriminator;
   private Level level;

   public IndigenousRecordFilter (Record record, Discriminator discriminator, Level level) {

      this.record = record;
      this.discriminator = discriminator;
      this.level = level;
   }

   public Record getRecord () {

      return this;
   }

   public Object getNativeLogEntry () {

      return this;
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

   public void addParameter (String key, Serializable value) {

      throw new UnsupportedOperationException();
   }

   public Parameter[] getParameters () {

      return record.getParameters();
   }

   public String getMessage () {

      return record.getMessage();
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