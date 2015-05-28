package org.smallmind.throng.wire.jms.jndi;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import org.smallmind.throng.wire.TransportException;
import org.smallmind.throng.wire.jms.ManagedObjectFactory;

public class JndiManagedObjectFactory implements ManagedObjectFactory {

  private JmsConnectionDetails messageConnectionDetails;

  public JndiManagedObjectFactory (JmsConnectionDetails jmsConnectionDetails) {

    this.messageConnectionDetails = jmsConnectionDetails;
  }

  @Override
  public Connection createConnection ()
    throws TransportException {

    try {

      Context javaEnvironment;
      QueueConnectionFactory queueConnectionFactory;

      javaEnvironment = messageConnectionDetails.getContextPool().getComponent();
      try {
        queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messageConnectionDetails.getConnectionFactoryName());
      } finally {
        javaEnvironment.close();
      }

      return queueConnectionFactory.createQueueConnection(messageConnectionDetails.getUserName(), messageConnectionDetails.getPassword());
    } catch (Exception exception) {
      throw new TransportException(exception);
    }
  }

  @Override
  public Destination getDestination ()
    throws TransportException {

    try {

      Context javaEnvironment;
      Queue queue;

      javaEnvironment = messageConnectionDetails.getContextPool().getComponent();
      try {
        queue = (Queue)javaEnvironment.lookup(messageConnectionDetails.getDestinationName());
      } finally {
        javaEnvironment.close();
      }

      return queue;
    } catch (Exception exception) {
      throw new TransportException(exception);
    }
  }
}