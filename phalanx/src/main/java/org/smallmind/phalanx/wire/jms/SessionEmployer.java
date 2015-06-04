package org.smallmind.phalanx.wire.jms;

import javax.jms.Destination;

public interface SessionEmployer {

  public abstract Destination getDestination ();

  public abstract String getMessageSelector ();
}