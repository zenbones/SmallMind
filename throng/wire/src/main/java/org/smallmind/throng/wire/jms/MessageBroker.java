package org.smallmind.throng.wire.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

public interface MessageBroker {

  public abstract ConnectionFactory lookupConnectionFactory (String path)
    throws Exception;

  public abstract Queue lookupQueue (String path)
    throws Exception;

  public abstract Topic lookupTopic (String path)
    throws Exception;

  public abstract void start ()
    throws Exception;

  public abstract void stop ()
    throws Exception;
}