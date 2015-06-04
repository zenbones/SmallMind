package org.smallmind.phalanx.wire.jms;

import javax.jms.Session;

public enum JmsAcknowledgeMode implements AcknowledgeMode {

  AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE), CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE), DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE);

  private int jmsValue;

  private JmsAcknowledgeMode (int jmsValue) {

    this.jmsValue = jmsValue;
  }

  public int getJmsValue () {

    return jmsValue;
  }
}