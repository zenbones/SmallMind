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
import org.smallmind.quorum.juggler.Juggler;
import org.smallmind.quorum.juggler.NoAvailableResourceException;
import org.smallmind.quorum.pool.connection.ConnectionInstance;
import org.smallmind.quorum.pool.connection.ConnectionInstanceFactory;
import org.smallmind.quorum.pool.connection.ConnectionPool;

public class MessageSenderConnectionInstanceFactory implements ConnectionInstanceFactory<MessageSender, MessageSender> {

  private Juggler<TransportManagedObjects, QueueConnection> queueConnectionJuggler;
  private Queue queue;
  private MessageStrategy messageStrategy;

  public MessageSenderConnectionInstanceFactory (Juggler<TransportManagedObjects, QueueConnection> queueConnectionJuggler, Queue queue, MessageStrategy messageStrategy) {

    this.queueConnectionJuggler = queueConnectionJuggler;
    this.queue = queue;
    this.messageStrategy = messageStrategy;
  }

  @Override
  public MessageSender rawInstance ()
    throws UnsupportedOperationException {

    throw new UnsupportedOperationException();
  }

  @Override
  public ConnectionInstance<MessageSender> createInstance (ConnectionPool<MessageSender> connectionPool)
    throws NoAvailableResourceException, JMSException {

    return new MessageSenderConnectionInstance(connectionPool, queueConnectionJuggler.pickResource(), queue, messageStrategy);
  }
}
