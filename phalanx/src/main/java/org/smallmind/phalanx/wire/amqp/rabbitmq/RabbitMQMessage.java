package org.smallmind.phalanx.wire.amqp.rabbitmq;

import com.rabbitmq.client.AMQP;

public class RabbitMQMessage {

  private final AMQP.BasicProperties properties;
  private final byte[] body;

  public RabbitMQMessage (AMQP.BasicProperties properties, byte[] body) {

    this.properties = properties;
    this.body = body;
  }

  public AMQP.BasicProperties getProperties () {

    return properties;
  }

  public byte[] getBody () {

    return body;
  }
}

