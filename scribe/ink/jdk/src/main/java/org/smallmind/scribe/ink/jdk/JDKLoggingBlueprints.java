package org.smallmind.scribe.ink.jdk;

import java.util.logging.Logger;
import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprints;

public class JDKLoggingBlueprints extends LoggingBlueprints {

   static {

      LoggerManager.addLoggingPackagePrefix("java.util.logging.");
   }

   public LoggerAdapter getLoggingAdapter (String name) {

      return new JDKLoggerAdapter(Logger.getLogger(name));
   }

   public Record filterRecord (Record record, Discriminator discriminator, Level level) {

      return new JDKRecordFilter(record, discriminator, level).getRecord();
   }

   public Record errorRecord (Record record, Throwable throwable, String message, Object... args) {

      LogicalContext logicalContext;

      logicalContext = new DefaultLogicalContext();
      logicalContext.fillIn();

      return new JDKRecordSubverter(record.getLoggerName(), null, Level.FATAL, null, logicalContext, throwable, message, args).getRecord();
   }
}