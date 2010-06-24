package org.smallmind.scribe.pen;

import java.io.Serializable;
import org.smallmind.scribe.pen.probe.ProbeReport;

public interface Record extends Serializable {

   public abstract Object getNativeLogEntry ();

   public abstract ProbeReport getProbeReport ();

   public abstract String getLoggerName ();

   public abstract Discriminator getDiscriminator ();

   public abstract Level getLevel ();

   public abstract Throwable getThrown ();

   public abstract String getMessage ();

   public abstract void addParameter (String key, Serializable value);

   public abstract Parameter[] getParameters ();

   public abstract LogicalContext getLogicalContext ();

   public abstract long getThreadID ();

   public abstract String getThreadName ();

   public abstract long getSequenceNumber ();

   public abstract long getMillis ();
}
