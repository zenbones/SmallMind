package org.smallmind.phalanx.wire.jms.hornetq;

import javax.jms.Session;
import org.smallmind.phalanx.wire.jms.AcknowledgeMode;
import org.hornetq.api.jms.HornetQJMSConstants;

public enum HornetQAcknowledgeMode implements AcknowledgeMode {

  AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE), CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE), DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE), PRE_ACKNOWLEDGE(HornetQJMSConstants.PRE_ACKNOWLEDGE);

  private int jmsValue;

  private HornetQAcknowledgeMode (int jmsValue) {

    this.jmsValue = jmsValue;
  }

  public int getJmsValue () {

    return jmsValue;
  }
}
