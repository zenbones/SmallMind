package org.smallmind.phalanx.wire.jms;

import javax.jms.JMSException;
import javax.jms.MessageProducer;

public class MessagePolicy {

  private AcknowledgeMode acknowledgeMode = JmsAcknowledgeMode.AUTO_ACKNOWLEDGE;
  private DeliveryMode deliveryMode = DeliveryMode.NON_PERSISTENT;
  private boolean disableMessageID = false;
  private boolean disableMessageTimestamp = false;
  private long timeToLive = 0;
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

  public void setTimeToLive (long timeToLive) {

    this.timeToLive = timeToLive;
  }

  public void setPriority (int priority) {

    this.priority = priority;
  }

  public void apply (MessageProducer producer)
    throws JMSException {

    producer.setDeliveryMode(deliveryMode.getJmsValue());
    producer.setDisableMessageID(disableMessageID);
    producer.setDisableMessageTimestamp(disableMessageTimestamp);
    producer.setTimeToLive(timeToLive);
    producer.setPriority(priority);
  }
}