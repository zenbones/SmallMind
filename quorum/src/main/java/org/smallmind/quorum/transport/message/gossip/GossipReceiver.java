/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message.gossip;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Topic;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.message.ConnectionFactor;
import org.smallmind.quorum.transport.message.MessagePlus;
import org.smallmind.quorum.transport.message.MessagePolicy;
import org.smallmind.quorum.transport.message.MessageStrategy;
import org.smallmind.quorum.transport.message.ReconnectionPolicy;
import org.smallmind.quorum.transport.message.TransportManagedObjects;

public class GossipReceiver {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final GossipListener gossipListener;
  private final GossipWorker[] gossipWorkers;

  public GossipReceiver (TransportManagedObjects gossipManagedObjects, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, MessageStrategy messageStrategy, int concurrencyLimit, GossipTarget... gossipTargets)
    throws IOException, JMSException, TransportException {

    TransferQueue<MessagePlus> messageRendezvous = new LinkedTransferQueue<MessagePlus>();
    HashMap<String, GossipTarget> targetMap = new HashMap<String, GossipTarget>();

    for (GossipTarget gossipTarget : gossipTargets) {
      targetMap.put(gossipTarget.getServiceInterface().getName(), gossipTarget);
    }

    gossipListener = new GossipListener(new ConnectionFactor(gossipManagedObjects, messagePolicy, reconnectionPolicy), (Topic)gossipManagedObjects.getDestination(), messageRendezvous);

    gossipWorkers = new GossipWorker[concurrencyLimit];
    for (int index = 0; index < gossipWorkers.length; index++) {
      new Thread(gossipWorkers[index] = new GossipWorker(messageStrategy, targetMap, messageRendezvous)).start();
    }
  }

  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      gossipListener.close();

      for (GossipWorker gossipWorker : gossipWorkers) {
        gossipWorker.stop();
      }
    }
  }
}

