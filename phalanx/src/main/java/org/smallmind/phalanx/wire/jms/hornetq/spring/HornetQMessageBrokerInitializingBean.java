package org.smallmind.phalanx.wire.jms.hornetq.spring;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.phalanx.wire.jms.MessageBroker;
import org.smallmind.phalanx.wire.jms.spring.ConnectionFactoryReference;
import org.smallmind.phalanx.wire.jms.spring.DestinationReference;
import org.springframework.beans.factory.InitializingBean;

public class HornetQMessageBrokerInitializingBean implements MessageBroker, InitializingBean {

  private final AtomicBoolean initialized = new AtomicBoolean(false);

  private EmbeddedJMS jmsServer;
  private File journalDirectory;
  private File pagingDirectory;
  private ConnectionFactoryReference connectionFactory;
  private DestinationReference[] destinations;
  private Map<String, HornetQAddressConfiguration> addressConfigurations;

  public HornetQMessageBrokerInitializingBean () {

  }

  public HornetQMessageBrokerInitializingBean (File journalDirectory, File pagingDirectory, ConnectionFactoryReference connectionFactory, DestinationReference[] destinations) {

    this.journalDirectory = journalDirectory;
    this.pagingDirectory = pagingDirectory;
    this.connectionFactory = connectionFactory;
    this.destinations = destinations;

    afterPropertiesSet();
  }

  public void setJournalDirectory (File journalDirectory) {

    this.journalDirectory = journalDirectory;
  }

  public void setPagingDirectory (File pagingDirectory) {

    this.pagingDirectory = pagingDirectory;
  }

  public void setConnectionFactory (ConnectionFactoryReference connectionFactory) {

    this.connectionFactory = connectionFactory;
  }

  public void setDestinations (DestinationReference[] destinations) {

    this.destinations = destinations;
  }

  public void setAddressConfigurations (Map<String, HornetQAddressConfiguration> addressConfigurations) {

    this.addressConfigurations = addressConfigurations;
  }

  @Override
  public void afterPropertiesSet () {

    if (initialized.compareAndSet(false, true)) {

      Configuration configuration;
      ConnectionFactoryConfiguration connectionFactoryConfiguration;
      JMSConfiguration jmsConfiguration;

      configuration = new ConfigurationImpl();

      for (Map.Entry<String, HornetQAddressConfiguration> addressConfigurationEntry : addressConfigurations.entrySet()) {

        AddressSettings addressSettings = new AddressSettings();

        addressSettings.setMaxSizeBytes(addressConfigurationEntry.getValue().getMaxSizeBytes());
        addressSettings.setPageSizeBytes(addressConfigurationEntry.getValue().getPageSizeBytes());
        addressSettings.setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE);
        configuration.getAddressesSettings().put(addressConfigurationEntry.getKey(), addressSettings);
      }

      configuration.setJournalDirectory(journalDirectory.getAbsolutePath());
      configuration.setPagingDirectory(pagingDirectory.getAbsolutePath());
      configuration.setPersistenceEnabled(false);
      configuration.setSecurityEnabled(false);
      configuration.getAcceptorConfigurations().add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
      configuration.getConnectorConfigurations().put("connector", new TransportConfiguration(NettyConnectorFactory.class.getName()));

      connectionFactoryConfiguration = new ConnectionFactoryConfigurationImpl(connectionFactory.getName(), false, Arrays.asList("connector"), connectionFactory.getPath());
      jmsConfiguration = new JMSConfigurationImpl();
      jmsConfiguration.getConnectionFactoryConfigurations().add(connectionFactoryConfiguration);

      for (DestinationReference destination : destinations) {
        switch (destination.getDestinationType()) {
          case QUEUE:
            jmsConfiguration.getQueueConfigurations().add(new JMSQueueConfigurationImpl(destination.getName(), destination.getSelector(), destination.isDurable(), destination.getPath()));
            break;
          case TOPIC:
            jmsConfiguration.getTopicConfigurations().add(new TopicConfigurationImpl(destination.getName(), destination.getPath()));
            break;
          default:
            throw new UnknownSwitchCaseException(destination.getDestinationType().name());
        }
      }

      jmsServer = new EmbeddedJMS();
      jmsServer.setConfiguration(configuration);
      jmsServer.setJmsConfiguration(jmsConfiguration);
    }
  }

  @Override
  public ConnectionFactory lookupConnectionFactory (String path)
    throws Exception {

    return (ConnectionFactory)jmsServer.lookup(path);
  }

  @Override
  public Queue lookupQueue (String path)
    throws Exception {

    return (Queue)jmsServer.lookup(path);
  }

  @Override
  public Topic lookupTopic (String path)
    throws Exception {

    return (Topic)jmsServer.lookup(path);
  }

  @Override
  public void start ()
    throws Exception {

    jmsServer.start();
  }

  @Override
  public void stop ()
    throws Exception {

    jmsServer.stop();
  }
}