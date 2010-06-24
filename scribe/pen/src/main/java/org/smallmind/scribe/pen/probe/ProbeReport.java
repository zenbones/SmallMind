package org.smallmind.scribe.pen.probe;

import java.io.Serializable;

public class ProbeReport implements Serializable {

   private Correlator correlator;
   private ProbeEntry probeEntry;
   private String title;
   private boolean first;

   public ProbeReport (Correlator correlator, String title, ProbeEntry probeEntry, boolean first) {

      this.correlator = correlator;
      this.title = title;
      this.probeEntry = probeEntry;
      this.first = first;
   }

   public boolean isFirst () {

      return first;
   }

   public Correlator getCorrelator () {

      return correlator;
   }

   public String getTitle () {

      return title;
   }

   public ProbeEntry getProbeEntry () {

      return probeEntry;
   }
}