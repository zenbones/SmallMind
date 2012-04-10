/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
      throw (ProbeException)new ProbeException("Probe has not been started").initCause(throwable);
    }
    else if (stopTime != 0) {
      throw (ProbeException)new ProbeException("Probe has already been terminated").initCause(throwable);
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

      probeException = (ProbeException)new ProbeException("Probe has not been started").initCause(throwable);

      throw probeException;
    }
    else if (stopTime != 0) {
      throw (ProbeException)new ProbeException("Attempting to abort Probe after at has already been stopped").initCause(throwable);
    }

    aborted = true;
    logger.log(discriminator, level, throwable, new ProbeReport(correlator, title, new CompleteOrAbortProbeEntry(ProbeStatus.ABORTED, this, startTime, System.currentTimeMillis()), first));
    statementList.clear();
    stopTime = -1;

    try {
      ProbeFactory.closeProbe(this);
    }
    catch (ProbeException probeException) {
      if (probeException.getCause() == probeException) {
        probeException.initCause(throwable);
      }

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