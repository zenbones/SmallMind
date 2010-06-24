package org.smallmind.scribe.pen.probe;

import java.util.LinkedList;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class Probe {

   private LinkedList<MetricMilieu> metricMilieuList;
   private LinkedList<Statement> statementList;
   public Logger logger;
   public Discriminator discriminator;
   public Level level;
   private Correlator correlator;
   private String title;
   private boolean first;
   private boolean aborted = false;
   private long startTime;
   private long stopTime;
   private int updateCount = 0;

   protected Probe (Logger logger, Discriminator discriminator, Level level, Correlator correlator, String title, boolean first) {

      this.logger = logger;
      this.discriminator = discriminator;
      this.level = level;
      this.correlator = correlator;
      this.title = title;
      this.first = first;

      metricMilieuList = new LinkedList<MetricMilieu>();
      statementList = new LinkedList<Statement>();
   }

   public void logMessage (String message, Object... args) {

      logger.log(discriminator, level, message, args);
   }

   public void logMessage (Discriminator discriminator, String message, Object... args) {

      logger.log(discriminator, level, message, args);
   }

   public void logMessage (Level level, String message, Object... args) {

      logger.log(discriminator, level, message, args);
   }

   public void logMessage (Discriminator discriminator, Level level, String message, Object... args) {

      logger.log(discriminator, level, message, args);
   }

   protected Correlator getCorrelator () {

      return correlator;
   }

   protected LinkedList<MetricMilieu> getMetricMilieuList () {

      return metricMilieuList;
   }

   public void addMetric (Metric metric) {

      metricMilieuList.add(new MetricMilieu(discriminator, level, metric));
   }

   public void addMetric (Discriminator discriminator, Metric metric) {

      metricMilieuList.add(new MetricMilieu(discriminator, level, metric));
   }

   public void addMetric (Level level, Metric metric) {

      metricMilieuList.add(new MetricMilieu(discriminator, level, metric));
   }

   public void addMetric (Discriminator discriminator, Level level, Metric metric) {

      metricMilieuList.add(new MetricMilieu(discriminator, level, metric));
   }

   protected LinkedList<Statement> getStatementList () {

      return statementList;
   }

   public void addStatement (String message, Object... args) {

      statementList.add(new Statement(discriminator, level, message, args));
   }

   public void addStatement (Discriminator discriminator, String message, Object... args) {

      statementList.add(new Statement(discriminator, level, message, args));
   }

   public void addStatement (Level level, String message, Object... args) {

      statementList.add(new Statement(discriminator, level, message, args));
   }

   public void addStatement (Discriminator discriminator, Level level, String message, Object... args) {

      statementList.add(new Statement(discriminator, level, message, args));
   }

   public boolean isAborted () {

      return aborted;
   }

   public Probe start ()
      throws ProbeException {

      if (startTime != 0) {
         throw new ProbeException("Probe has already been started");
      }

      startTime = System.currentTimeMillis();

      return this;
   }

   public void update ()
      throws ProbeException {

      update(null);
   }

   public void update (Throwable throwable)
      throws ProbeException {

      if (startTime == 0) {

         ProbeException probeException;

         probeException = new ProbeException("Probe has not been started");
         probeException.initCause(throwable);

         throw probeException;
      }
      else if (stopTime != 0) {

         ProbeException probeException;

         probeException = new ProbeException("Probe has already been terminated");
         probeException.initCause(throwable);

         throw probeException;
      }

      logger.log(discriminator, level, throwable, new ProbeReport(correlator, title, new UpdateProbeEntry(this, System.currentTimeMillis(), updateCount++), first));
      statementList.clear();
   }

   public void stop ()
      throws ProbeException {

      if (startTime == 0) {
         throw new ProbeException("Probe has not been started");
      }
      else if (stopTime < 0) {

         throw new ProbeException("Attempting to stop Probe after it has already been aborted");
      }

      stopTime = System.currentTimeMillis();
      logger.log(discriminator, level, new ProbeReport(correlator, title, new CompleteOrAbortProbeEntry(ProbeStatus.COMPLETED, this, startTime, stopTime), first));
      statementList.clear();

      ProbeFactory.closeProbe(this);
   }

   public void abort ()
      throws ProbeException {

      abort(null);
   }

   public void abort (Throwable throwable)
      throws ProbeException {

      if (startTime == 0) {

         ProbeException probeException;

         probeException = new ProbeException("Probe has not been started");
         probeException.initCause(throwable);

         throw probeException;
      }
      else if (stopTime != 0) {

         ProbeException probeException;

         probeException = new ProbeException("Attempting to abort Probe after at has already been stopped");
         probeException.initCause(throwable);

         throw probeException;
      }

      aborted = true;
      logger.log(discriminator, level, throwable, new ProbeReport(correlator, title, new CompleteOrAbortProbeEntry(ProbeStatus.ABORTED, this, startTime, System.currentTimeMillis()), first));
      statementList.clear();
      stopTime = -1;

      try {
         ProbeFactory.closeProbe(this);
      }
      catch (ProbeException probeException) {

         probeException.initCause(throwable);

         throw probeException;
      }
   }

   public int hashCode () {

      return correlator.hashCode();
   }

   public boolean equals (Object obj) {

      return (obj instanceof Probe) && correlator.equals(((Probe)obj).getCorrelator());
   }
}