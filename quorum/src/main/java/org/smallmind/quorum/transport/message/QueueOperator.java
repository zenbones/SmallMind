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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

public class QueueOperator {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final QueueSession requestSession;
  private final QueueSender requestSender;

  public QueueOperator (QueueConnection requestConnection, Queue requestQueue, MessagePolicy messagePolicy)
    throws JMSException {

    requestSession = requestConnection.createQueueSession(false, messagePolicy.getAcknowledgeMode().getJmsValue());
    requestSender = requestSession.createSender(requestQueue);
    messagePolicy.apply(requestSender);
  }

  public QueueSession getRequestSession () {

    return requestSession;
  }

  public void send (Message message)
    throws JMSException {

    requestSender.send(message);
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestSender.close();
      requestSession.close();
    }
  }
}
