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

import javax.jms.Queue;
import javax.jms.QueueConnection;
import org.smallmind.quorum.juggler.Juggler;
import org.smallmind.quorum.juggler.ResourceException;
import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPoolConfig;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;
import org.smallmind.quorum.transport.TransportException;

public class MessageTransmitter {

  private Juggler<TransportManagedObjects, QueueConnection> queueConnectionJuggler;
  private ConnectionPool<MessageSender> messageSenderPool;

  public MessageTransmitter (TransportManagedObjects managedObjects, MessagePolicy messagePolicy, MessageStrategy messageStrategy, int connectionCount, MessageSenderPoolConfig messageSenderPoolConfig)
    throws ResourceException, ConnectionPoolException, TransportException {

    queueConnectionJuggler = new Juggler<TransportManagedObjects, QueueConnection>(TransportManagedObjects.class, 60, new QueueConnectionJugglingPinFactory(), managedObjects, connectionCount);
    messageSenderPool = new ConnectionPool<MessageSender>("", new MessageSenderConnectionInstanceFactory(queueConnectionJuggler, (Queue)managedObjects.getDestination(), messagePolicy, messageStrategy), new ConnectionPoolConfig(messageSenderPoolConfig));

    queueConnectionJuggler.initialize();
    messageSenderPool.startup();
    queueConnectionJuggler.startup();
  }

  public MessageSender borrowMessageSender ()
    throws ConnectionPoolException {

    return messageSenderPool.getConnection();
  }

  public void returnMessageSender (MessageSender messageSender) {

    messageSenderPool.returnInstance(messageSender.getConnectionInstance());
  }

  public void close ()
    throws ConnectionPoolException {

    queueConnectionJuggler.shutdown();
    messageSenderPool.shutdown();
    queueConnectionJuggler.deconstruct();
  }
}
