/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.quorum.transport.message;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Message;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricInteraction;
import org.smallmind.scribe.pen.LoggerManager;

public class ReceptionWorker implements Runnable {

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final MessageStrategy messageStrategy;
  private final Map<String, MessageTarget> targetMap;
  private final TransferQueue<MessagePlus> messageRendezvous;
  private final ConcurrentLinkedQueue<TopicOperator> operatorQueue;

  public ReceptionWorker (MessageStrategy messageStrategy, Map<String, MessageTarget> targetMap, TransferQueue<MessagePlus> messageRendezvous, ConcurrentLinkedQueue<TopicOperator> operatorQueue) {

    this.messageStrategy = messageStrategy;
    this.targetMap = targetMap;
    this.messageRendezvous = messageRendezvous;
    this.operatorQueue = operatorQueue;
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
          InstrumentationManager.instrumentWithChronometer(TransportManager.getTransport(), Clocks.EPOCH.getClock().getTimeNanoseconds() - idleStart, TimeUnit.NANOSECONDS, new MetricProperty("event", MetricInteraction.WORKER_IDLE.getDisplay()));

          TopicOperator topicOperator;

          if ((topicOperator = operatorQueue.poll()) == null) {
            throw new TransportException("Unable to take a TopicOperator, which should never happen - please contact your system administrator");
          }

          try {

            Message responseMessage;
            String transmissionInstance;

            if ((transmissionInstance = messagePlus.getMessage().getStringProperty(MessageProperty.INSTANCE.getKey())) == null) {
              throw new TransportException("Missing message property(%s)", MessageProperty.INSTANCE.getKey());
            }

            try {

              MessageTarget messageTarget;
              String serviceSelector;

              if ((serviceSelector = messagePlus.getMessage().getStringProperty(MessageProperty.SERVICE.getKey())) == null) {
                throw new TransportException("Missing message property(%s)", MessageProperty.SERVICE.getKey());
              } else if ((messageTarget = targetMap.get(serviceSelector)) == null) {
                throw new TransportException("Unknown service selector(%s)", serviceSelector);
              }

              responseMessage = messageTarget.handleMessage(topicOperator.getTopicSession(), messageStrategy, messagePlus.getMessage());
            } catch (Exception exception) {
              responseMessage = messageStrategy.wrapInMessage(topicOperator.getTopicSession(), exception);
              responseMessage.setBooleanProperty(MessageProperty.EXCEPTION.getKey(), true);
            }

            responseMessage.setJMSCorrelationID(messagePlus.getMessage().getJMSMessageID());
            responseMessage.setStringProperty(MessageProperty.INSTANCE.getKey(), transmissionInstance);
            responseMessage.setLongProperty(MessageProperty.CLOCK.getKey(), System.currentTimeMillis());

            topicOperator.publish(responseMessage);
          } catch (Exception exception) {
            LoggerManager.getLogger(ReceptionWorker.class).error(exception);
          } finally {
            operatorQueue.add(topicOperator);
            InstrumentationManager.publishMetricContext();
          }

          idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();
        }
      }
    } catch (Exception exception) {
      LoggerManager.getLogger(ReceptionWorker.class).error(exception);
    } finally {
      exitLatch.countDown();
    }
  }
}
