package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractAppender implements Appender {

   private Formatter formatter;
   private ErrorHandler errorHandler;
   private ConcurrentLinkedQueue<Filter> filterList;
   private String name;

   public AbstractAppender () {

      this(null, null, null);
   }

   public AbstractAppender (Formatter formatter, ErrorHandler errorHandler) {

      this(null, formatter, errorHandler);
   }

   public AbstractAppender (String name, Formatter formatter, ErrorHandler errorHandler) {

      this.name = name;
      this.formatter = formatter;
      this.errorHandler = errorHandler;

      filterList = new ConcurrentLinkedQueue<Filter>();
   }

   public abstract void handleOutput (String formattedOutput)
      throws Exception;

   public void handleError (ErrorHandler errorHandler, Record record, Exception exception) {

      errorHandler.process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
   }

   public String getName () {

      return name;
   }

   public void setName (String name) {

      this.name = name;
   }

   public synchronized void clearFilters () {

      filterList.clear();
   }

   public synchronized void setFilter (Filter filter) {

      filterList.clear();
      filterList.add(filter);
   }

   public synchronized void setFilters (List<Filter> replacementFilterList) {

      filterList.clear();
      filterList.addAll(replacementFilterList);
   }

   public synchronized void addFilter (Filter filter) {

      filterList.add(filter);
   }

   public synchronized Filter[] getFilters () {

      Filter[] filters;

      filters = new Filter[filterList.size()];
      filterList.toArray(filters);

      return filters;
   }

   public void setErrorHandler (ErrorHandler errorHandler) {

      this.errorHandler = errorHandler;
   }

   public ErrorHandler getErrorHandler () {

      return errorHandler;
   }

   public void setFormatter (Formatter formatter) {

      this.formatter = formatter;
   }

   public Formatter getFormatter () {

      return formatter;
   }

   public void publish (Record record) {

      try {
         for (Filter filter : filterList) {
            if (!filter.willLog(record)) {
               return;
            }
         }

         if (formatter != null) {
            handleOutput(formatter.format(record, filterList));
         }
         else if (requiresFormatter()) {
            throw new LoggerException("No formatter set for log output on this appender(%s)", this.getClass().getCanonicalName());
         }
      }
      catch (Throwable throwable) {
         if (errorHandler == null) {
            throwable.printStackTrace();
         }
         else {
            handleError(errorHandler, record, (Exception)throwable);
         }
      }
   }

   public void close ()
      throws LoggerException {
   }
}