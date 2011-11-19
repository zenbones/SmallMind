/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.cloud.service.messaging.spring;

import java.util.HashMap;
import java.util.List;
import javax.jms.JMSException;
import javax.naming.NamingException;
import org.smallmind.quorum.pool2.ConnectionPool;
import org.smallmind.quorum.pool2.ConnectionPoolException;
import org.smallmind.quorum.transport.messaging.MessagingConnectionDetails;
import org.smallmind.quorum.transport.messaging.MessagingTransmitter;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceDispatcherInitializingBean implements InitializingBean, DisposableBean {

  private static final HashMap<String, MessagingTransmitter> MESSAGING_TRANSMITTER_MAP = new HashMap<String, MessagingTransmitter>();

  private ConnectionPool javaEnvironmentPool;
  private List<String> serviceSelectorList;
  private String jmsUser;
  private String jmsCredentials;
  private String destinationEnvPath;
  private String factoryEnvPath;
  private boolean closed = false;
  private int transmissionPoolSize;

  public static MessagingTransmitter getMessagingTransmitter (String serviceSelector) {

    return MESSAGING_TRANSMITTER_MAP.get(serviceSelector);
  }

  public void setJmsUser (String jmsUser) {

    this.jmsUser = jmsUser;
  }

  public void setJmsCredentials (String jmsCredentials) {

    this.jmsCredentials = jmsCredentials;
  }

  public void setJavaEnvironmentPool (ConnectionPool javaEnvironmentPool) {

    this.javaEnvironmentPool = javaEnvironmentPool;
  }

  public void setDestinationEnvPath (String destinationEnvPath) {

    this.destinationEnvPath = destinationEnvPath;
  }

  public void setFactoryEnvPath (String factoryEnvPath) {

    this.factoryEnvPath = factoryEnvPath;
  }

  public void setTransmissionPoolSize (int transmissionPoolSize) {

    this.transmissionPoolSize = transmissionPoolSize;
  }

  public void setServiceSelectorList (List<String> serviceSelectorList) {

    this.serviceSelectorList = serviceSelectorList;
  }

  public void afterPropertiesSet ()
    throws NoSuchMethodException, NamingException, JMSException, ConnectionPoolException {

    MessagingConnectionDetails connectionDetails;

    for (String serviceSelector : serviceSelectorList) {
      connectionDetails = new MessagingConnectionDetails(javaEnvironmentPool, destinationEnvPath, factoryEnvPath, jmsUser, jmsCredentials, transmissionPoolSize, serviceSelector);
      MESSAGING_TRANSMITTER_MAP.put(serviceSelector, new MessagingTransmitter(connectionDetails));
    }
  }

  public synchronized void close () {

    if (!closed) {
      closed = true;

      for (MessagingTransmitter messagingTransmitter : MESSAGING_TRANSMITTER_MAP.values()) {
        try {
          messagingTransmitter.close();
        }
        catch (JMSException jmsException) {
          LoggerManager.getLogger(ServiceDispatcherInitializingBean.class).error(jmsException);
        }
      }
    }
  }

  public void destroy () {

    close();
  }

  public void finalize () {

    close();
  }
}
