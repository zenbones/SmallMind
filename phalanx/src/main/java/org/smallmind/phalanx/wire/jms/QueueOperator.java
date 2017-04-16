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
package org.smallmind.phalanx.wire.jms;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import org.smallmind.scribe.pen.LoggerManager;

public class QueueOperator implements SessionEmployer, MessageHandler {

  private final ConnectionManager connectionManager;
  private final Queue requestQueue;

  public QueueOperator (ConnectionManager connectionManager, Queue queue) {

    this.connectionManager = connectionManager;
    this.requestQueue = queue;
  }

  @Override
  public Destination getDestination () {

    return requestQueue;
  }

  @Override
  public String getMessageSelector () {

    return null;
  }

  @Override
  public BytesMessage createMessage ()
    throws JMSException {

    return connectionManager.getSession(this).createBytesMessage();
  }

  @Override
  public void send (Message message)
    throws JMSException {

    connectionManager.getProducer(this).send(message);
    LoggerManager.getLogger(QueueOperator.class).debug("queue message sent(%s)...", message.getJMSMessageID());
  }
}