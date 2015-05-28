package org.smallmind.throng.wire.jms.hornetq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import org.smallmind.throng.wire.TransportException;
import org.smallmind.throng.wire.jms.ManagedObjectFactory;
import org.smallmind.throng.wire.jms.spring.DestinationType;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class HornetQManagedObjectFactory implements ManagedObjectFactory {

  private ConnectionFactory connectionFactory;
  private Destination destination;
  private String user;
  private String password;

  public HornetQManagedObjectFactory (String user, String password, String destinationName, DestinationType destinationType, TransportConfiguration... transportConfigurations) {

    this.user = user;
    this.password = password;

    switch (destinationType) {
      case QUEUE:
        destination = HornetQJMSClient.createQueue(destinationName);
        break;
      case TOPIC:
        destination = HornetQJMSClient.createTopic(destinationName);
        break;
      default:
        throw new UnknownSwitchCaseException(destinationType.name());
    }

    connectionFactory = HornetQJMSClient.createConnectionFactoryWithHA(JMSFactoryType.CF, transportConfigurations);
  }

  @Override
  public Connection createConnection ()
    throws TransportException {

    try {
      return connectionFactory.createConnection(user, password);
    } catch (JMSException jmsException) {
      throw new TransportException(jmsException);
    }
  }

  @Override
  public Destination getDestination () {

    return destination;
  }
}
