package org.smallmind.throng.wire.jms;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import org.smallmind.scribe.pen.LoggerManager;

public class QueueOperator implements SessionEmployer, MessageHandler {

  private final ConnectionManager connectionManager;
  private final Queue requestQueue;

  public QueueOperator (ConnectionManager connectionManager, Queue queue) {

    this.connectionManager = connectionManager;
    this.requestQueue = queue;
  }

  @Override
  public Destination getDestination () {

    return requestQueue;
  }

  @Override
  public String getMessageSelector () {

    return null;
  }

  @Override
  public BytesMessage createMessage ()
    throws JMSException {

    return connectionManager.getSession(this).createBytesMessage();
  }

  @Override
  public void send (Message message)
    throws JMSException {

    connectionManager.getProducer(this).send(message);
    LoggerManager.getLogger(QueueOperator.class).debug("queue message sent(%s)...", message.getJMSMessageID());
  }
}