package org.smallmind.cloud.transport.messaging;

import javax.jms.Message;
import javax.jms.Session;

public interface MessageTarget {

   public abstract Message handleMessage (Session session, Message message)
      throws Exception;

   public abstract void logError (Throwable throwable);
}
