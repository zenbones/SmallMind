package org.smallmind.phalanx.wire.jms;

public enum DeliveryMode {

  PERSISTENT(javax.jms.DeliveryMode.PERSISTENT), NON_PERSISTENT(javax.jms.DeliveryMode.NON_PERSISTENT);

  private int jmsValue;

  private DeliveryMode (int jmsValue) {

    this.jmsValue = jmsValue;
  }

  public int getJmsValue () {

    return jmsValue;
  }
}
