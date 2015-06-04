package org.smallmind.phalanx.wire.jms;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionManager implements ExceptionListener {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ManagedObjectFactory managedObjectFactory;
  private final MessagePolicy messagePolicy;
  private final ReconnectionPolicy reconnectionPolicy;
  private final ConcurrentHashMap<SessionEmployer, Session> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionEmployer, MessageProducer> producerMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SessionEmployer, MessageConsumer> consumerMap = new ConcurrentHashMap<>();

  private Connection connection;

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

  public void stop ()
    throws JMSException {

    if (connection != null) {
      connection.stop();
    }
  }

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

        TransportException transportException = new TransportException("Unable to reconnection within max attempts(%d)", reconnectionPolicy.getReconnectionAttempts());

        LoggerManager.getLogger(ConnectionManager.class).error((lastException != null) ? transportException.initCause(lastException) : transportException);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}

