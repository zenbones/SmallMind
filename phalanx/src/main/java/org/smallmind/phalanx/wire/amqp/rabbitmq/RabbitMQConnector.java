package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConnector {

  private final ConnectionFactory connectionFactory;
  private final Address[] addresses;

  public RabbitMQConnector (ConnectionFactory connectionFactory, Address... addresses) {

    this.connectionFactory = connectionFactory;
    this.addresses = addresses;
  }

  public Connection getConnection ()
    throws IOException {

    return connectionFactory.newConnection(addresses);
  }
}