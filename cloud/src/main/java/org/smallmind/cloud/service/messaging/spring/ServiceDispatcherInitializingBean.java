/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;
import org.smallmind.quorum.transport.message.MessageConnectionDetails;
import org.smallmind.quorum.transport.message.MessageTransmitter;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceDispatcherInitializingBean implements InitializingBean, DisposableBean, FactoryBean<MessageTransmitter> {

  private MessageTransmitter messagingTransmitter;
  private ConnectionPool<Context> javaEnvironmentPool;
  private String jmsUser;
  private String jmsCredentials;
  private String destinationEnvPath;
  private String factoryEnvPath;
  private boolean closed = false;
  private int transmissionPoolSize;

  public void setJmsUser (String jmsUser) {

    this.jmsUser = jmsUser;
  }

  public void setJmsCredentials (String jmsCredentials) {

    this.jmsCredentials = jmsCredentials;
  }

  public void setJavaEnvironmentPool (ConnectionPool<Context> javaEnvironmentPool) {

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

  public void afterPropertiesSet ()
    throws ConnectionPoolException, NamingException, JMSException {

    MessageConnectionDetails connectionDetails;

    connectionDetails = new MessageConnectionDetails(javaEnvironmentPool, destinationEnvPath, factoryEnvPath, jmsUser, jmsCredentials);
    messagingTransmitter = new MessageTransmitter(connectionDetails, transmissionPoolSize);
  }

  @Override
  public MessageTransmitter getObject () {

    return messagingTransmitter;
  }

  @Override
  public Class<?> getObjectType () {

    return MessageTransmitter.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  public synchronized void close () {

    if (!closed) {
      closed = true;

      try {
        messagingTransmitter.close();
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ServiceDispatcherInitializingBean.class).error(exception);
      }
    }
  }

  public void destroy () {

    close();
  }
}
