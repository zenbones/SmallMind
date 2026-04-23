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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jakarta.jms.Connection;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Manages the lifecycle of a single JMS {@link Connection} and caches the sessions, producers,
 * and consumers created for each {@link SessionEmployer}.
 *
 * <p>When the underlying connection raises a JMS exception, the manager attempts to
 * re-establish it according to the supplied {@link ReconnectionPolicy}, rebuilding all
 * registered consumers automatically.
 */
public class ConnectionManager implements ExceptionListener {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ManagedObjectFactory managedObjectFactory;
  private final MessagePolicy messagePolicy;
  private final ReconnectionPolicy reconnectionPolicy;
  private final ConcurrentHashMap<SessionEmployer, Session> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionEmployer, MessageProducer> producerMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionEmployer, MessageConsumer> consumerMap = new ConcurrentHashMap<>();

  private Connection connection;

  /**
   * Constructs the manager and opens an initial JMS connection.
   *
   * @param managedObjectFactory factory used to create JMS connections and resolve destinations
   * @param messagePolicy        producer settings applied to every {@link jakarta.jms.MessageProducer} created
   * @param reconnectionPolicy   reconnection timing and attempt-limit configuration
   * @throws TransportException if the factory cannot produce a connection
   * @throws JMSException       if JMS resources cannot be initialised
   */
  public ConnectionManager (ManagedObjectFactory managedObjectFactory, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy)
    throws TransportException, JMSException {

    this.managedObjectFactory = managedObjectFactory;
    this.messagePolicy = messagePolicy;
    this.reconnectionPolicy = reconnectionPolicy;

    createConnection();
  }

  private void createConnection ()
    throws TransportException, JMSException {

    if (connection != null) {
      try {
        connection.stop();
        connection.close();
      } catch (JMSException jmsException) {
        LoggerManager.getLogger(ConnectionManager.class).error(jmsException);
      }
    }

    connection = managedObjectFactory.createConnection();
    connection.setExceptionListener(this);
  }

  /**
   * Returns the cached JMS session for the given employer, creating and caching one on first access.
   *
   * @param sessionEmployer employer whose session is needed
   * @return JMS {@link Session} bound to this employer
   * @throws JMSException if a new session cannot be created
   */
  public Session getSession (SessionEmployer sessionEmployer)
    throws JMSException {

    Session session;

    lock.readLock().lock();
    try {
      if ((session = sessionMap.get(sessionEmployer)) == null) {
        sessionMap.put(sessionEmployer, session = connection.createSession(false, messagePolicy.getAcknowledgeMode().getJmsValue()));
      }
    } finally {
      lock.readLock().unlock();
    }

    return session;
  }

  /**
   * Returns the cached {@link MessageProducer} for the given employer, creating and
   * configuring one via {@link MessagePolicy#apply(MessageProducer)} on first access.
   *
   * @param sessionEmployer employer whose producer is needed
   * @return JMS {@link MessageProducer} targeting the employer's destination
   * @throws JMSException if a new producer cannot be created
   */
  public MessageProducer getProducer (SessionEmployer sessionEmployer)
    throws JMSException {

    MessageProducer producer;

    lock.readLock().lock();
    try {
      if ((producer = producerMap.get(sessionEmployer)) == null) {
        producerMap.put(sessionEmployer, producer = getSession(sessionEmployer).createProducer(sessionEmployer.getDestination()));
        messagePolicy.apply(producer);
      }
    } finally {
      lock.readLock().unlock();
    }

    return producer;
  }

  /**
   * Creates a {@link jakarta.jms.MessageConsumer} for the employer's destination and selector,
   * registers the employer as its {@link jakarta.jms.MessageListener}, and starts the connection.
   *
   * @param sessionEmployer employer that provides the destination, optional message selector,
   *                        and serves as the {@link jakarta.jms.MessageListener}
   * @throws JMSException if the consumer cannot be created or the connection cannot be started
   */
  public void createConsumer (SessionEmployer sessionEmployer)
    throws JMSException {

    lock.readLock().lock();
    try {

      MessageConsumer consumer;
      String selector;

      consumerMap.put(sessionEmployer, consumer = (((selector = sessionEmployer.getMessageSelector()) == null) ? getSession(sessionEmployer).createConsumer(sessionEmployer.getDestination()) : getSession(sessionEmployer).createConsumer(sessionEmployer.getDestination(), selector, false)));
      consumer.setMessageListener((MessageListener)sessionEmployer);
      connection.start();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Starts message delivery on the managed connection, if one exists.
   *
   * @throws JMSException if the connection cannot be started
   */
  public void start ()
    throws JMSException {

    if (connection != null) {
      connection.start();
    }
  }

  /**
   * Suspends message delivery on the managed connection, if one exists.
   *
   * @throws JMSException if the connection cannot be stopped
   */
  public void stop ()
    throws JMSException {

    if (connection != null) {
      connection.stop();
    }
  }

  /**
   * Closes all cached producers, consumers, and sessions, then closes the underlying connection.
   *
   * @throws JMSException if any resource cannot be closed
   */
  public void close ()
    throws JMSException {

    if (connection != null) {
      for (MessageProducer producer : producerMap.values()) {
        producer.close();
      }
      for (MessageConsumer consumer : consumerMap.values()) {
        consumer.close();
      }
      for (Session session : sessionMap.values()) {
        session.close();
      }

      connection.close();
    }
  }

  /**
   * Handles a JMS provider exception by attempting to rebuild the connection, sessions, and
   * consumers according to the configured {@link ReconnectionPolicy}.  Logs success or failure.
   *
   * @param jmsException the exception raised by the JMS provider
   */
  @Override
  public void onException (JMSException jmsException) {

    lock.writeLock().lock();
    try {

      Exception lastException = null;
      boolean success = false;
      int reconnectionCount = 0;

      LoggerManager.getLogger(ConnectionManager.class).error(jmsException);

      while ((!success) && ((reconnectionPolicy.getReconnectionAttempts() < 0) || (reconnectionCount++ < reconnectionPolicy.getReconnectionAttempts()))) {
        try {
          Thread.sleep(reconnectionPolicy.getReconnectionDelayMilliseconds());

          createConnection();

          sessionMap.clear();
          producerMap.clear();

          for (SessionEmployer sessionEmployer : consumerMap.keySet()) {
            createConsumer(sessionEmployer);
          }

          success = true;
        } catch (Exception exception) {
          lastException = exception;
        }
      }

      if (success) {
        LoggerManager.getLogger(ConnectionManager.class).info("Successful reconnection after JMS provider failure");
      } else {

        TransportException transportException = new TransportException("Unable to reconnect within max attempts(%d)", reconnectionPolicy.getReconnectionAttempts());

        LoggerManager.getLogger(ConnectionManager.class).error((lastException != null) ? transportException.initCause(lastException) : transportException);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
