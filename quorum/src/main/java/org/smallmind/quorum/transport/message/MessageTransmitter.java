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
package org.smallmind.quorum.transport.message;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.component.ComponentFactory;
import org.smallmind.quorum.pool.component.ComponentPool;
import org.smallmind.quorum.pool.component.ComponentPoolException;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;

public class MessageTransmitter {

  private QueueConnection queueConnection;
  private ComponentPool<MessageSender> messageSenderPool;

  public MessageTransmitter (MessageConnectionDetails messagingConnectionDetails, int transmissionPoolSize)
    throws ConnectionPoolException, NamingException, JMSException {

    Context javaEnvironment;
    Queue queue;
    QueueConnectionFactory queueConnectionFactory;

    javaEnvironment = messagingConnectionDetails.getContextPool().getConnection();
    try {
      queue = (Queue)javaEnvironment.lookup(messagingConnectionDetails.getDestinationName());
      queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messagingConnectionDetails.getConnectionFactoryName());
    }
    finally {
      javaEnvironment.close();
    }

    queueConnection = queueConnectionFactory.createQueueConnection(messagingConnectionDetails.getUserName(), messagingConnectionDetails.getPassword());

    messageSenderPool = new ComponentPool<MessageSender>(new MessageSenderComponentFactory(queue), transmissionPoolSize, 0);

    queueConnection.start();
  }

  public MessageSender borrowMessageSender ()
    throws ComponentPoolException {

    return messageSenderPool.getComponent();
  }

  public void returnMessageSender (MessageSender messageSender) {

    messageSenderPool.returnComponent(messageSender);
  }

  public void close ()
    throws InterruptedException, JMSException {

    queueConnection.stop();
    messageSenderPool.close();
    queueConnection.close();
  }

  private class MessageSenderComponentFactory implements ComponentFactory<MessageSender> {

    private Queue queue;

    public MessageSenderComponentFactory (Queue queue) {

      this.queue = queue;
    }

    public MessageSender createComponent ()
      throws JMSException {

      return new MessageSender(queueConnection, queue);
    }
  }
}
