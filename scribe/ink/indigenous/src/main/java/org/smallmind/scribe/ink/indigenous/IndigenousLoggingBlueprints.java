package org.smallmind.scribe.ink.indigenous;

import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprints;

public class IndigenousLoggingBlueprints extends LoggingBlueprints {

   public LoggerAdapter getLoggingAdapter (String name) {

      return new IndigenousLoggerAdapter(name);
   }

   public Record filterRecord (Record record, Discriminator discriminator, Level level) {

      return new IndigenousRecordFilter(record, discriminator, level).getRecord();
   }

   public Record errorRecord (Record record, Throwable throwable, String message, Object... args) {

      IndigenousRecord indigenousRecord;
      LogicalContext logicalContext;

      indigenousRecord = new IndigenousRecord(record.getLoggerName(), null, Level.FATAL, null, throwable, message, args);
      logicalContext = new DefaultLogicalContext();
      logicalContext.fillIn();
      indigenousRecord.setLogicalContext(logicalContext);

      return indigenousRecord.getRecord();
   }
}