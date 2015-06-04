package org.smallmind.phalanx.wire.jms;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import org.smallmind.scribe.pen.LoggerManager;

public class TopicOperator implements SessionEmployer, MessageHandler {

  private final ConnectionManager connectionManager;
  private final Topic topic;

  public TopicOperator (ConnectionManager connectionManager, Topic topic) {

    this.connectionManager = connectionManager;
    this.topic = topic;
  }

  @Override
  public Destination getDestination () {

    return topic;
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
    LoggerManager.getLogger(TopicOperator.class).debug("topic message sent(%s)...", message.getJMSMessageID());
  }
}