package org.smallmind.scribe.pen;

import java.util.List;

public interface Appender {

   public abstract void setName (String name);

   public abstract String getName ();

   public abstract void setFilter (Filter filter);

   public abstract void setFilters (List<Filter> filterList);

   public abstract void clearFilters ();

   public abstract void addFilter (Filter filter);

   public abstract Filter[] getFilters ();

   public abstract void setErrorHandler (ErrorHandler errorHandler);

   public abstract ErrorHandler getErrorHandler ();

   public abstract void setFormatter (Formatter formatter);

   public abstract Formatter getFormatter ();

   public abstract boolean requiresFormatter ();

   public abstract void publish (Record record);

   public abstract void close ()
      throws LoggerException;
}
