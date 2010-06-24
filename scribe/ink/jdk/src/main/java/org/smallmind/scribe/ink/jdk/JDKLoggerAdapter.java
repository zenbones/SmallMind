package org.smallmind.scribe.ink.jdk;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.DefaultLogicalContext;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LogicalContext;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.probe.ProbeReport;

public class JDKLoggerAdapter implements LoggerAdapter {

   private final Logger logger;

   private ConcurrentLinkedQueue<Filter> filterList;
   private ConcurrentLinkedQueue<Enhancer> enhancerList;
   private boolean autoFillLogicalContext = false;

   public JDKLoggerAdapter (Logger logger) {

      this.logger = logger;

      filterList = new ConcurrentLinkedQueue<Filter>();
      enhancerList = new ConcurrentLinkedQueue<Enhancer>();
   }

   public String getName () {

      return logger.getName();
   }

   public boolean getAutoFillLogicalContext () {

      return autoFillLogicalContext;
   }

   public void setAutoFillLogigicalContext (boolean autoFillLogicalContext) {

      this.autoFillLogicalContext = autoFillLogicalContext;
   }

   public synchronized void addFilter (Filter filter) {

      synchronized (logger) {
         filterList.add(filter);
         logger.setFilter(new JDKFilterWrapper(filter));
      }
   }

   public synchronized void clearFilters () {

      filterList.clear();
      logger.setFilter(null);
   }

   public synchronized void addAppender (Appender appender) {

      logger.addHandler(new JDKAppenderWrapper(appender));
   }

   public synchronized void clearAppenders () {

      for (Handler handler : logger.getHandlers()) {
         logger.removeHandler(handler);
      }
   }

   public void addEnhancer (Enhancer enhancer) {

      enhancerList.add(enhancer);
   }

   public void clearEnhancers () {

      enhancerList.clear();
   }

   public Level getLevel () {

      return (logger.getLevel() == null) ? Level.INFO : JDKLevelTranslator.getLevel(logger.getLevel());
   }

   public void setLevel (Level level) {

      logger.setLevel(JDKLevelTranslator.getLog4JLevel(level));
   }

   public void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args) {

      JDKRecordSubverter recordSubverter;
      LogicalContext logicalContext;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         if ((logicalContext = willLog(discriminator, level)) != null) {
            recordSubverter = new JDKRecordSubverter(logger.getName(), discriminator, level, null, logicalContext, throwable, message, args);
            enhanceRecord(recordSubverter.getRecord());
            logger.log(recordSubverter);
         }
      }
   }

   public void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport) {

      JDKRecordSubverter recordSubverter;
      LogicalContext logicalContext;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         if ((logicalContext = willLog(discriminator, level)) != null) {
            recordSubverter = new JDKRecordSubverter(logger.getName(), discriminator, level, probeReport, logicalContext, throwable, (probeReport.getTitle() == null) ? "Probe Report" : probeReport.getTitle());
            enhanceRecord(recordSubverter.getRecord());
            logger.log(recordSubverter);
         }
      }
   }

   public void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object) {

      JDKRecordSubverter recordSubverter;
      LogicalContext logicalContext;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         if ((logicalContext = willLog(discriminator, level)) != null) {
            recordSubverter = new JDKRecordSubverter(logger.getName(), discriminator, level, null, logicalContext, throwable, (object == null) ? null : object.toString());
            enhanceRecord(recordSubverter.getRecord());
            logger.log(recordSubverter);
         }
      }
   }

   private LogicalContext willLog (Discriminator discriminator, Level level) {

      LogicalContext logicalContext;
      Record filterRecord;

      logicalContext = new DefaultLogicalContext();
      if (getAutoFillLogicalContext()) {
         logicalContext.fillIn();
      }

      if (!((logger.getFilter() == null) && filterList.isEmpty())) {
         filterRecord = new JDKRecordSubverter(logger.getName(), discriminator, level, null, logicalContext, null, null).getRecord();

         if (logger.getFilter() != null) {
            if (!logger.getFilter().isLoggable((LogRecord)filterRecord.getNativeLogEntry())) {
               return null;
            }
         }

         if (!filterList.isEmpty()) {
            for (Filter filter : filterList) {
               if (!filter.willLog(filterRecord)) {
                  return null;
               }
            }
         }
      }

      return logicalContext;
   }

   private void enhanceRecord (Record record) {

      for (Enhancer enhancer : enhancerList) {
         enhancer.enhance(record);
      }
   }
}