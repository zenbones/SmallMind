package org.smallmind.scribe.ink.indigenous;

import java.util.concurrent.ConcurrentLinkedQueue;
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

public class IndigenousLoggerAdapter implements LoggerAdapter {

   private ConcurrentLinkedQueue<Filter> filterList;
   private ConcurrentLinkedQueue<Appender> appenderList;
   private ConcurrentLinkedQueue<Enhancer> enhancerList;
   private Level level = Level.INFO;
   private String name;
   private boolean autoFillLogicalContext = false;

   public IndigenousLoggerAdapter (String name) {

      this.name = name;

      filterList = new ConcurrentLinkedQueue<Filter>();
      appenderList = new ConcurrentLinkedQueue<Appender>();
      enhancerList = new ConcurrentLinkedQueue<Enhancer>();
   }

   public String getName () {

      return name;
   }

   public boolean getAutoFillLogicalContext () {

      return autoFillLogicalContext;
   }

   public void setAutoFillLogigicalContext (boolean autoFillLogicalContext) {

      this.autoFillLogicalContext = autoFillLogicalContext;
   }

   public void addFilter (Filter filter) {

      filterList.add(filter);
   }

   public void clearFilters () {

      filterList.clear();
   }

   public void addAppender (Appender appender) {

      appenderList.add(appender);
   }

   public void clearAppenders () {

      appenderList.clear();
   }

   public void addEnhancer (Enhancer enhancer) {

      enhancerList.add(enhancer);
   }

   public void clearEnhancers () {

      enhancerList.clear();
   }

   public Level getLevel () {

      return level;
   }

   public void setLevel (Level level) {

      this.level = level;
   }

   public void logMessage (Discriminator discriminator, Level level, Throwable throwable, String message, Object... args) {

      IndigenousRecord indigenousRecord;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         indigenousRecord = new IndigenousRecord(name, discriminator, level, null, throwable, message, args);
         if (willLog(indigenousRecord)) {
            completeLogOperation(indigenousRecord);
         }
      }
   }

   public void logProbe (Discriminator discriminator, Level level, Throwable throwable, ProbeReport probeReport) {

      IndigenousRecord indigenousRecord;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         indigenousRecord = new IndigenousRecord(name, discriminator, level, probeReport, throwable, (probeReport.getTitle() == null) ? "Probe Report" : probeReport.getTitle());
         if (willLog(indigenousRecord)) {
            completeLogOperation(indigenousRecord);
         }
      }
   }

   public void logMessage (Discriminator discriminator, Level level, Throwable throwable, Object object) {

      IndigenousRecord indigenousRecord;

      if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
         indigenousRecord = new IndigenousRecord(name, discriminator, level, null, throwable, (object == null) ? null : object.toString());
         if (willLog(indigenousRecord)) {
            completeLogOperation(indigenousRecord);
         }
      }
   }

   private boolean willLog (IndigenousRecord indigenousRecord) {

      LogicalContext logicalContext;

      logicalContext = new DefaultLogicalContext();
      if (getAutoFillLogicalContext()) {
         logicalContext.fillIn();
      }

      indigenousRecord.setLogicalContext(logicalContext);

      if (!filterList.isEmpty()) {
         for (Filter filter : filterList) {
            if (!filter.willLog(indigenousRecord)) {
               return false;
            }
         }
      }

      return true;
   }

   private void completeLogOperation (Record record) {

      for (Enhancer enhancer : enhancerList) {
         enhancer.enhance(record);
      }

      for (Appender appender : appenderList) {
         appender.publish(record);
      }
   }
}