package org.smallmind.scribe.pen.probe;

import java.io.Serializable;
import java.util.LinkedList;

public abstract class ProbeEntry implements Serializable {

   private ProbeStatus probeStatus;
   private LinkedList<MetricMilieu> metricMilieuList;
   private LinkedList<Statement> statementList;

   public ProbeEntry (ProbeStatus probeStatus, Probe probe) {

      this.probeStatus = probeStatus;

      metricMilieuList = new LinkedList<MetricMilieu>();

      for (MetricMilieu metricMilieu : probe.getMetricMilieuList()) {
         metricMilieuList.add(new MetricMilieu(metricMilieu));
      }

      statementList = new LinkedList<Statement>(probe.getStatementList());
   }

   public ProbeStatus getProbeStatus () {

      return probeStatus;
   }

   public MetricMilieu[] getMetricMilieus () {

      MetricMilieu[] metricMilieus;

      metricMilieus = new MetricMilieu[metricMilieuList.size()];
      metricMilieuList.toArray(metricMilieus);

      return metricMilieus;
   }

   public Statement[] getStatements () {

      Statement[] statements;

      statements = new Statement[statementList.size()];
      statementList.toArray(statements);

      return statements;
   }
}
