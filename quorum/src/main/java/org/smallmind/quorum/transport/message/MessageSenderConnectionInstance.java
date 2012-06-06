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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import org.smallmind.quorum.pool.connection.ConnectionInstance;
import org.smallmind.quorum.pool.connection.ConnectionPool;

public class MessageSenderConnectionInstance implements ConnectionInstance<MessageSender> {

  private final ConnectionPool<MessageSender> connectionPool;
  private final MessageSender messageSender;
  private final AtomicReference<StackTraceElement[]> stackTraceReference = new AtomicReference<StackTraceElement[]>();
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public MessageSenderConnectionInstance (ConnectionPool<MessageSender> connectionPool, QueueConnection queueConnection, Queue queue, MessagePolicy messagePolicy, MessageStrategy messageStrategy)
    throws JMSException {

    this.connectionPool = connectionPool;

    messageSender = new MessageSender(this, queueConnection, queue, messagePolicy, messageStrategy);
  }

  @Override
  public boolean validate () {

    return true;
  }

  @Override
  public MessageSender serve () {

    if (connectionPool.getConnectionPoolConfig().isExistentiallyAware()) {
      stackTraceReference.set(Thread.currentThread().getStackTrace());
    }

    return messageSender;
  }

  @Override
  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      if (connectionPool.getConnectionPoolConfig().isExistentiallyAware()) {
        stackTraceReference.set(null);
      }

      messageSender.close();
    }
  }

  @Override
  public StackTraceElement[] getExistentialStackTrace () {

    return stackTraceReference.get();
  }
}
