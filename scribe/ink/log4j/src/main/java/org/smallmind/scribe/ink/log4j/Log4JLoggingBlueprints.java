package org.smallmind.scribe.ink.log4j;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.LoggingBlueprints;

public class Log4JLoggingBlueprints extends LoggingBlueprints {

   static {

      LoggerManager.addLoggingPackagePrefix("org.apache.log4j.");
   }

   public LoggerAdapter getLoggingAdapter (String name) {

      return new Log4JLoggerAdapter(Logger.getLogger(name));
   }

   public Record filterRecord (Record record, Discriminator discriminator, Level level) {

      return new Log4JRecordFilter(record, discriminator, level).getRecord();
   }

   public Record errorRecord (Record record, Throwable throwable, String message, Object... args) {

      LogicalContext logicalContext;

      logicalContext = new DefaultLogicalContext();
      logicalContext.fillIn();

      return new Log4JRecordSubverter((Logger)((LoggingEvent)record.getNativeLogEntry()).getLogger(), null, Level.FATAL, null, logicalContext, throwable, message, args).getRecord();
   }
}
