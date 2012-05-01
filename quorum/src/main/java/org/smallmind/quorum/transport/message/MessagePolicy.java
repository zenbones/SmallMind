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
import javax.jms.MessageProducer;

public class MessagePolicy {

  private AcknowledgeMode acknowledgeMode;
  private DeliveryMode deliveryMode;
  private boolean disableMessageID = false;
  private boolean disableMessageTimestamp = false;
  private long timeToLive = 0;
  private int priority = 4;

  public void setAcknowledgeMode (AcknowledgeMode acknowledgeMode) {

    this.acknowledgeMode = acknowledgeMode;
  }

  public AcknowledgeMode getAcknowledgeMode () {

    return acknowledgeMode;
  }

  public void setDeliveryMode (DeliveryMode deliveryMode) {

    this.deliveryMode = deliveryMode;
  }

  public void setDisableMessageID (boolean disableMessageID) {

    this.disableMessageID = disableMessageID;
  }

  public void setDisableMessageTimestamp (boolean disableMessageTimestamp) {

    this.disableMessageTimestamp = disableMessageTimestamp;
  }

  public void setTimeToLive (long timeToLive) {

    this.timeToLive = timeToLive;
  }

  public void setPriority (int priority) {

    this.priority = priority;
  }

  public void apply (MessageProducer producer)
    throws JMSException {

    producer.setDisableMessageID(disableMessageID);
    producer.setDisableMessageTimestamp(disableMessageTimestamp);
    producer.setTimeToLive(timeToLive);
    producer.setDeliveryMode(deliveryMode.getJmsValue());
    producer.setPriority(priority);
  }
}
