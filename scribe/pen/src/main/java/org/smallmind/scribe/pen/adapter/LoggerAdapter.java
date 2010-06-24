package org.smallmind.scribe.pen.adapter;

import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.probe.ProbeReport;

public interface LoggerAdapter {

   public abstract String getName ();

   public abstract boolean getAutoFillLogicalContext ();

   public abstract void setAutoFillLogigicalContext (boolean autoFillLogicalContext);

   public abstract void addFilter (Filter filter);

   public abstract void clearFilters ();

   public abstract void addAppender (Appender appender);

   public abstract void clearAppenders ();

   public abstract void addEnhancer (Enhancer enhancer);

   public abstract void clearEnhancers ();

   public abstract Level getLevel ();

   public abstract void setLevel (Level level);

   public abstract void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args);

   public abstract void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport);

   public abstract void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object);
}
