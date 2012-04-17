package org.smallmind.quorum.transport.message;

import java.io.Serializable;
import javax.jms.Message;
import javax.jms.Session;

public interface MessageStrategy {

  public abstract Message wrapInMessage (Session session, Serializable serializable)
    throws Exception;

  public abstract Object unwrapFromMessage (Message message)
    throws Exception;
}
