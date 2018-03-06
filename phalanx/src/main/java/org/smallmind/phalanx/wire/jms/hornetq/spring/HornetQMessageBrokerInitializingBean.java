/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.jms.hornetq.spring;

import java.nio.file.Path;
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
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.phalanx.wire.jms.MessageBroker;
import org.smallmind.phalanx.wire.jms.spring.ConnectionFactoryReference;
import org.smallmind.phalanx.wire.jms.spring.DestinationReference;
import org.springframework.beans.factory.InitializingBean;

public class HornetQMessageBrokerInitializingBean implements MessageBroker, InitializingBean {

  private final AtomicBoolean initialized = new AtomicBoolean(false);

  private EmbeddedJMS jmsServer;
  private Path journalDirectory;
  private Path pagingDirectory;
  private ConnectionFactoryReference connectionFactory;
  private DestinationReference[] destinations;
  private Map<String, HornetQAddressConfiguration> addressConfigurations;

  public HornetQMessageBrokerInitializingBean () {

  }

  public HornetQMessageBrokerInitializingBean (Path journalDirectory, Path pagingDirectory, ConnectionFactoryReference connectionFactory, DestinationReference[] destinations) {

    this.journalDirectory = journalDirectory;
    this.pagingDirectory = pagingDirectory;
    this.connectionFactory = connectionFactory;
    this.destinations = destinations;

    afterPropertiesSet();
  }

  public void setJournalDirectory (Path journalDirectory) {

    this.journalDirectory = journalDirectory;
  }

  public void setPagingDirectory (Path pagingDirectory) {

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

      configuration.setJournalDirectory(PathUtility.asNormalizedString(journalDirectory));
      configuration.setPagingDirectory(PathUtility.asNormalizedString(pagingDirectory));
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
  public ConnectionFactory lookupConnectionFactory (String path) {

    return (ConnectionFactory)jmsServer.lookup(path);
  }

  @Override
  public Queue lookupQueue (String path) {

    return (Queue)jmsServer.lookup(path);
  }

  @Override
  public Topic lookupTopic (String path) {

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