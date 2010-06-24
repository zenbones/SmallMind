package org.smallmind.scribe.pen.probe;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.UniqueId;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class ProbeStack {

   private LinkedList<Probe> probeList;
   private byte[] threadIdentifier;
   private byte[] parentIdentifier;
   private int instance = 0;

   public ProbeStack () {

      this(null);
   }

   public ProbeStack (byte[] parentIdentifier) {

      this.parentIdentifier = parentIdentifier;

      threadIdentifier = UniqueId.newInstance().asByteArray();
      probeList = new LinkedList<Probe>();
   }

   public byte[] getCurrentIdentifier () {

      if (probeList.isEmpty()) {

         return null;
      }

      return probeList.getFirst().getCorrelator().getIdentifier();
   }

   public Probe peek () {

      if (probeList.isEmpty()) {

         return null;
      }

      return probeList.getFirst();
   }

   public Probe push (Logger logger, Discriminator discriminator, Level level, String title) {

      Probe probe;

      probeList.addFirst(probe = new Probe(logger, discriminator, level, new Correlator(threadIdentifier, probeList.isEmpty() ? parentIdentifier : probeList.getFirst().getCorrelator().getIdentifier(), UniqueId.newInstance().asByteArray(), probeList.size(), instance++), title, probeList.isEmpty()));

      return probe;
   }

   public void pop (Probe probe)
      throws ProbeException {

      if (!probe.equals(probeList.getFirst())) {
         throw new ProbeException("Out of order Probe(%s) termination", probe.getCorrelator().getIdentifier());
      }

      probeList.removeFirst();
   }
}