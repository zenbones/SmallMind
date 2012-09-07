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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import org.smallmind.scribe.pen.LoggerManager;

public class TopicOperator implements SessionEmployer {

  private final ConnectionFactor responseConnectionFactor;
  private final Topic responseTopic;

  public TopicOperator (ConnectionFactor responseConnectionFactor, Topic responseTopic) {

    this.responseConnectionFactor = responseConnectionFactor;
    this.responseTopic = responseTopic;
  }

  @Override
  public Destination getDestination () {

    return responseTopic;
  }

  @Override
  public String getMessageSelector () {

    return null;
  }

  public Session getResponseSession ()
    throws JMSException {

    return responseConnectionFactor.getSession(this);
  }

  public void publish (Message message)
    throws JMSException {

    responseConnectionFactor.getProducer(this).send(message);
    LoggerManager.getLogger(TopicOperator.class).debug("response message sent(%s)...", message.getJMSMessageID());
  }
}
