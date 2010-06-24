package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousAppender implements Appender, Runnable {

   private Appender internalAppender;
   private LinkedBlockingQueue<Record> publishQueue;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private boolean terminated = false;

   public AsynchronousAppender (Appender internalAppender) {

      Thread publishThread;

      this.internalAppender = internalAppender;

      publishQueue = new LinkedBlockingQueue<Record>();

      publishThread = new Thread(this);
      publishThread.setDaemon(true);
      publishThread.start();
   }

   public void setName (String name) {

      internalAppender.setName(name);
   }

   public String getName () {

      return internalAppender.getName();
   }

   public void clearFilters () {

      internalAppender.clearFilters();
   }

   public synchronized void setFilter (Filter filter) {

      internalAppender.setFilter(filter);
   }

   public void setFilters (List<Filter> filterList) {

      internalAppender.setFilters(filterList);
   }

   public void addFilter (Filter filter) {

      internalAppender.addFilter(filter);
   }

   public Filter[] getFilters () {

      return internalAppender.getFilters();
   }

   public ErrorHandler getErrorHandler () {

      return internalAppender.getErrorHandler();
   }

   public void setErrorHandler (ErrorHandler errorHandler) {

      internalAppender.setErrorHandler(errorHandler);
   }

   public void setFormatter (Formatter formatter) {

      internalAppender.setFormatter(formatter);
   }

   public Formatter getFormatter () {

      return internalAppender.getFormatter();
   }

   public boolean requiresFormatter () {

      return internalAppender.requiresFormatter();
   }

   public void publish (Record record) {

      try {
         if (finished.get()) {
            throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
         }

         publishQueue.put(record);
      }
      catch (Exception exception) {
         if (internalAppender.getErrorHandler() == null) {
            exception.printStackTrace();
         }
         else {
            internalAppender.getErrorHandler().process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
         }
      }
   }

   public void close ()
      throws LoggerException {

      try {
         finish();
      }
      catch (InterruptedException interuptedException) {
         throw new LoggerException(interuptedException);
      }

      internalAppender.close();
   }

   public void finish ()
      throws InterruptedException {

      if (!finished.get()) {
         finished.set(true);

         while (!terminated) {
            Thread.sleep(300);
         }
      }
   }

   protected void finalize ()
      throws InterruptedException {

      finish();
   }

   public void run () {

      Record record;

      try {
         while (!(finished.get() && publishQueue.isEmpty())) {
            if ((record = publishQueue.poll(1000, TimeUnit.MILLISECONDS)) != null) {
               internalAppender.publish(record);
            }
         }
      }
      catch (InterruptedException interruptedException) {
         finished.set(true);
      }

      terminated = true;
   }
}