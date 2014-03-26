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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricInteraction;
import org.smallmind.quorum.transport.message.MessagePlus;
import org.smallmind.quorum.transport.message.MessageProperty;
import org.smallmind.quorum.transport.message.MessageStrategy;
import org.smallmind.scribe.pen.LoggerManager;

public class GossipWorker implements Runnable {

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final MessageStrategy messageStrategy;
  private final Map<String, GossipTarget> targetMap;
  private final TransferQueue<MessagePlus> messageRendezvous;

  public GossipWorker (MessageStrategy messageStrategy, Map<String, GossipTarget> targetMap, TransferQueue<MessagePlus> messageRendezvous) {

    this.messageStrategy = messageStrategy;
    this.targetMap = targetMap;
    this.messageRendezvous = messageRendezvous;
  }

  public void stop ()
    throws InterruptedException {

    stopped.set(true);
    exitLatch.await();
  }

  @Override
  public void run () {

    long idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();

    try {
      while (!stopped.get()) {

        MessagePlus messagePlus;

        if ((messagePlus = messageRendezvous.poll(1, TimeUnit.SECONDS)) != null) {

          InstrumentationManager.setMetricContext(messagePlus.getMetricContext());
          InstrumentationManager.instrumentWithChronometer(TransportManager.getTransport(), Clocks.EPOCH.getClock().getTimeNanoseconds() - idleStart, TimeUnit.NANOSECONDS, new MetricProperty("gossip", "true"), new MetricProperty("event", MetricInteraction.WORKER_IDLE.getDisplay()));

          try {

            GossipTarget gossipTarget;
            String serviceSelector;

            if ((serviceSelector = messagePlus.getMessage().getStringProperty(MessageProperty.SERVICE.getKey())) == null) {
              throw new TransportException("Missing message property(%s)", MessageProperty.SERVICE.getKey());
            }
            else if ((gossipTarget = targetMap.get(serviceSelector)) == null) {
              throw new TransportException("Unknown service selector(%s)", serviceSelector);
            }

            gossipTarget.handleMessage(messageStrategy, messagePlus.getMessage());
          }
          catch (Exception exception) {
            LoggerManager.getLogger(GossipWorker.class).error(exception);
          }
          finally {
            InstrumentationManager.publishMetricContext();
          }

          idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();
        }
      }
    }
    catch (Exception exception) {
      LoggerManager.getLogger(GossipWorker.class).error(exception);
    }
    finally {
      exitLatch.countDown();
    }
  }
}
