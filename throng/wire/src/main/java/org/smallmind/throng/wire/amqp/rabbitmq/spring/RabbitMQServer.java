package org.smallmind.throng.wire.amqp.rabbitmq.spring;

public class RabbitMQServer {

  private String host;
  private int port = 5672;

  public String getHost () {

    return host;
  }

  public void setHost (String host) {

    this.host = host;
  }

  public int getPort () {

    return port;
  }

  public void setPort (int port) {

    this.port = port;
  }
}