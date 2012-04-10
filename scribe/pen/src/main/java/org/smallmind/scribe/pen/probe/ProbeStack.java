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
import org.smallmind.nutsnbolts.util.UniqueId;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class ProbeStack {

   private LinkedList<Probe> probeList;
   private byte[] threadIdentifier;
   private byte[] parentIdentifier;
   private int instance = 0;

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