package org.smallmind.scribe.pen.adapter;

import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;

public abstract class LoggingBlueprints {

   public abstract LoggerAdapter getLoggingAdapter (String name);

   public abstract Record filterRecord (Record record, Discriminator discriminator, Level level);

   public abstract Record errorRecord (Record record, Throwable throwable, String message, Object... args);
}
