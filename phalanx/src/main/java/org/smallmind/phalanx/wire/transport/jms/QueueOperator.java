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

import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link SessionEmployer} and {@link MessageHandler} implementation that sends messages
 * to a point-to-point JMS {@link Queue}.
 *
 * <p>Instances are pooled by {@link JmsRequestTransport} for talk-mode transmissions.
 */
public class QueueOperator implements SessionEmployer, MessageHandler {

  private final ConnectionManager connectionManager;
  private final Queue requestQueue;

  /**
   * Constructs a {@code QueueOperator} bound to the specified queue.
   *
   * @param connectionManager shared connection manager that provides the JMS session and producer
   * @param queue             destination queue for all outbound messages sent through this operator
   */
  public QueueOperator (ConnectionManager connectionManager, Queue queue) {

    this.connectionManager = connectionManager;
    this.requestQueue = queue;
  }

  /**
   * Returns the queue destination that this operator targets.
   *
   * @return the JMS {@link Queue} supplied at construction
   */
  @Override
  public Destination getDestination () {

    return requestQueue;
  }

  /**
   * Returns {@code null} because queue consumers created from this operator require no
   * message selector.
   *
   * @return {@code null}
   */
  @Override
  public String getMessageSelector () {

    return null;
  }

  /**
   * Creates a new empty {@link BytesMessage} using the session provided by the connection manager.
   *
   * @return a new, empty {@link BytesMessage}
   * @throws JMSException if the session cannot create the message
   */
  @Override
  public BytesMessage createMessage ()
    throws JMSException {

    return connectionManager.getSession(this).createBytesMessage();
  }

  /**
   * Sends {@code message} to the queue via the producer provided by the connection manager and
   * logs the outbound message id at debug level.
   *
   * @param message the populated message to send
   * @throws JMSException if the producer cannot dispatch the message
   */
  @Override
  public void send (Message message)
    throws JMSException {

    connectionManager.getProducer(this).send(message);
    LoggerManager.getLogger(QueueOperator.class).debug("queue message sent(%s)...", message.getJMSMessageID());
  }
}
