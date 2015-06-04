package org.smallmind.phalanx.wire.jms;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;

public interface MessageHandler {

  public abstract BytesMessage createMessage ()
    throws JMSException;

  public abstract void send (Message message)
    throws JMSException;
}
