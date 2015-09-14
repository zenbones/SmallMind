package org.smallmind.phalanx.wire.amqp.rabbitmq.spring;

import java.util.ArrayList;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.phalanx.wire.amqp.rabbitmq.RabbitMQConnector;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class RabbitMQConnectorFactory implements FactoryBean<RabbitMQConnector>, InitializingBean {

  /*
  rabbitmq.host.<#> (required)
  rabbitmq.port.<#> (defaults to 5672)
  */

  private RabbitMQConnector rabbitMQConnector;
  private String username;
  private String password;
  private int heartbeatSeconds;

  public void setUsername (String username) {

    this.username = username;
  }

  public void setPassword (String password) {

    this.password = password;
  }

  public void setHeartbeatSeconds (int heartbeatSeconds) {

    this.heartbeatSeconds = heartbeatSeconds;
  }

  @Override
  public Class<?> getObjectType () {

    return RabbitMQConnector.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public RabbitMQConnector getObject () throws Exception {

    return rabbitMQConnector;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();
    ConnectionFactory connectionFactory;
    Address[] addresses;
    ArrayList<RabbitMQServer> serverList = new ArrayList<>();
    int addressIndex = 0;

    for (String key : springPropertyAccessor.getKeySet()) {
      if (key.startsWith("rabbitmq.host.")) {

        int index = Integer.valueOf(key.substring("rabbitmq.host.".length()));

        while (serverList.size() < index + 1) {
          serverList.add(new RabbitMQServer());
        }

        serverList.get(index).setHost(springPropertyAccessor.asString(key));
      } else if (key.startsWith("rabbitmq.port.")) {

        int index = Integer.valueOf(key.substring("rabbitmq.host.".length()));

        while (serverList.size() < index + 1) {
          serverList.add(new RabbitMQServer());
        }

        serverList.get(index).setPort(springPropertyAccessor.asInt(key).get());
      }
    }

    addresses = new Address[serverList.size()];
    for (RabbitMQServer server : serverList) {
      addresses[addressIndex++] = new Address(server.getHost(), server.getPort());
    }

    connectionFactory = new ConnectionFactory();
    connectionFactory.setAutomaticRecoveryEnabled(true);
    connectionFactory.setRequestedHeartbeat(heartbeatSeconds);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);

    rabbitMQConnector = new RabbitMQConnector(connectionFactory, addresses);
  }
}