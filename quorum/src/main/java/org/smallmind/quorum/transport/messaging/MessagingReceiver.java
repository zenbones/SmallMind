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
package org.smallmind.quorum.transport.messaging;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;

public class MessagingReceiver {

  private AtomicBoolean stopped = new AtomicBoolean(false);
  private MessageTarget messageTarget;
  private QueueConnection queueConnection;
  private MessagingWorker[] messagingWorkers;

  public MessagingReceiver (MessageTarget messageTarget, MessagingConnectionDetails messagingConnectionDetails, String serviceSelector, int serviceConcurrencyLimit)
    throws ConnectionPoolException, NamingException, JMSException {

    Context javaEnvironment;
    Queue queue;
    QueueConnectionFactory queueConnectionFactory;

    this.messageTarget = messageTarget;

    javaEnvironment = messagingConnectionDetails.getContextPool().getConnection();
    try {
      queue = (Queue)javaEnvironment.lookup(messagingConnectionDetails.getDestinationName());
      queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messagingConnectionDetails.getConnectionFactoryName());
    }
    finally {
      javaEnvironment.close();
    }

    queueConnection = queueConnectionFactory.createQueueConnection(messagingConnectionDetails.getUserName(), messagingConnectionDetails.getPassword());

    messagingWorkers = new MessagingWorker[serviceConcurrencyLimit];
    for (int count = 0; count < messagingWorkers.length; count++) {
      messagingWorkers[count] = new MessagingWorker(queueConnection, queue, messageTarget, serviceSelector);
    }

    queueConnection.start();
  }

  public synchronized void close () {

    if (stopped.compareAndSet(false, true)) {
      try {
        queueConnection.stop();

        for (MessagingWorker messageWorker : messagingWorkers) {
          messageWorker.close();
        }

        queueConnection.close();
      }
      catch (JMSException jmsException) {
        messageTarget.logError(jmsException);
      }
    }
  }
}
