/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.transport.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * JMS {@link MessageListener} that receives inbound service-invocation requests from a queue or
 * topic and forwards them to a {@link JmsResponseTransport} worker pool for execution.
 *
 * <p>A JMS message-selector is constructed from the {@code serviceGroup} and optional
 * {@code instanceId} at construction time and applied to the consumer, ensuring that only
 * messages addressed to the correct service and instance are delivered.
 */
public class RequestListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final JmsResponseTransport jmsResponseTransport;
  private final ConnectionManager requestConnectionManager;
  private final Destination requestDestination;
  private final String selector;

  /**
   * Constructs the listener, builds a JMS message-selector from the service group and optional
   * instance id, creates the consumer, and starts the connection.
   *
   * @param jmsResponseTransport     response transport to which received messages are forwarded
   * @param requestConnectionManager connection manager that provides the JMS session and consumer
   * @param requestDestination       destination (queue or topic) to subscribe to
   * @param serviceGroup             service group name included in the message-selector filter
   * @param instanceId               optional instance id for whisper-mode filtering; {@code null}
   *                                 for shout and talk modes
   * @throws JMSException if consumer creation fails
   */
  public RequestListener (JmsResponseTransport jmsResponseTransport, ConnectionManager requestConnectionManager, Destination requestDestination, String serviceGroup, String instanceId)
    throws JMSException {

    this.jmsResponseTransport = jmsResponseTransport;
    this.requestConnectionManager = requestConnectionManager;
    this.requestDestination = requestDestination;

    selector = (instanceId == null) ? WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "'" : WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "' AND " + WireProperty.INSTANCE_ID.getKey() + "='" + instanceId + "'";

    requestConnectionManager.createConsumer(this);
  }

  /**
   * Returns the destination (queue or topic) that this listener is subscribed to.
   *
   * @return the JMS {@link Destination} supplied at construction
   */
  @Override
  public Destination getDestination () {

    return requestDestination;
  }

  /**
   * Returns the JMS message-selector that filters messages by service group and, for whisper
   * mode, by instance id.
   *
   * @return non-null JMS selector string
   */
  @Override
  public String getMessageSelector () {

    return selector;
  }

  /**
   * Resumes message consumption by starting the underlying connection.
   *
   * @throws JMSException if the connection cannot be started
   */
  public void play ()
    throws JMSException {

    requestConnectionManager.start();
  }

  /**
   * Suspends message consumption by stopping the underlying connection without releasing resources.
   *
   * @throws JMSException if the connection cannot be stopped
   */
  public void pause ()
    throws JMSException {

    requestConnectionManager.stop();
  }

  /**
   * Stops and closes the underlying connection manager.  Idempotent; safe to call multiple times.
   *
   * @throws JMSException if stopping or closing the connection manager fails
   */
  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnectionManager.stop();
      requestConnectionManager.close();
    }
  }

  /**
   * Receives an inbound request message, records the queue transit time as a Claxon metric,
   * and submits the message to the {@link JmsResponseTransport} for asynchronous execution.
   *
   * @param message the inbound JMS message containing an encoded invocation signal
   */
  @Override
  public void onMessage (final Message message) {

    try {

      long timeInQueue = System.currentTimeMillis() - message.getLongProperty(WireProperty.CLOCK.getKey());

      LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", message.getJMSMessageID(), timeInQueue);
      Instrument.with(RequestListener.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.REQUEST_TRANSIT_TIME.getDisplay())).update((timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS);

      jmsResponseTransport.execute(message);
    } catch (Throwable throwable) {
      LoggerManager.getLogger(RequestListener.class).error(throwable);
    }
  }
}
