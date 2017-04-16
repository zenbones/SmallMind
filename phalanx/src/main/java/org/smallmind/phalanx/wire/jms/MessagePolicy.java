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

import javax.jms.JMSException;
import javax.jms.MessageProducer;

public class MessagePolicy {

  private AcknowledgeMode acknowledgeMode = JmsAcknowledgeMode.AUTO_ACKNOWLEDGE;
  private DeliveryMode deliveryMode = DeliveryMode.NON_PERSISTENT;
  private boolean disableMessageID = false;
  private boolean disableMessageTimestamp = false;
  private int timeToLiveSeconds = 0;
  private int priority = 4;

  public AcknowledgeMode getAcknowledgeMode () {

    return acknowledgeMode;
  }

  public void setAcknowledgeMode (AcknowledgeMode acknowledgeMode) {

    this.acknowledgeMode = acknowledgeMode;
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

  public void setTimeToLiveSeconds (int timeToLiveSeconds) {

    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public void setPriority (int priority) {

    this.priority = priority;
  }

  public void apply (MessageProducer producer)
    throws JMSException {

    producer.setDeliveryMode(deliveryMode.getJmsValue());
    producer.setDisableMessageID(disableMessageID);
    producer.setDisableMessageTimestamp(disableMessageTimestamp);
    producer.setTimeToLive(timeToLiveSeconds * 1000);
    producer.setPriority(priority);
  }
}