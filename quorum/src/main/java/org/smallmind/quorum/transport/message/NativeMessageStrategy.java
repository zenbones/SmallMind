package org.smallmind.quorum.transport.message;

import java.io.Serializable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

public class NativeMessageStrategy implements MessageStrategy {

  @Override
  public Message wrapInMessage (Session session, Serializable serializable)
    throws JMSException {

    return session.createObjectMessage(serializable);
  }

  @Override
  public Object unwrapFromMessage (Message message)
    throws JMSException {

    return ((ObjectMessage)message).getObject();
  }
}
