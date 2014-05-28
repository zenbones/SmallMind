/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message.jndi;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.message.TransportManagedObjects;

public class JNDITransportManagedObjects implements TransportManagedObjects {

  private MessageConnectionDetails messageConnectionDetails;

  public JNDITransportManagedObjects (MessageConnectionDetails messageConnectionDetails) {

    this.messageConnectionDetails = messageConnectionDetails;
  }

  @Override
  public Connection createConnection ()
    throws TransportException {

    try {

      Context javaEnvironment;
      QueueConnectionFactory queueConnectionFactory;

      javaEnvironment = messageConnectionDetails.getContextPool().getComponent();
      try {
        queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messageConnectionDetails.getConnectionFactoryName());
      }
      finally {
        javaEnvironment.close();
      }

      return queueConnectionFactory.createQueueConnection(messageConnectionDetails.getUserName(), messageConnectionDetails.getPassword());
    }
    catch (Exception exception) {
      throw new TransportException(exception);
    }
  }

  @Override
  public Destination getDestination ()
    throws TransportException {

    try {

      Context javaEnvironment;
      Queue queue;

      javaEnvironment = messageConnectionDetails.getContextPool().getComponent();
      try {
        queue = (Queue)javaEnvironment.lookup(messageConnectionDetails.getDestinationName());
      }
      finally {
        javaEnvironment.close();
      }

      return queue;
    }
    catch (Exception exception) {
      throw new TransportException(exception);
    }
  }
}
