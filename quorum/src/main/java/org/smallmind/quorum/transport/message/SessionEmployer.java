package org.smallmind.quorum.transport.message;

import javax.jms.Destination;

public interface SessionEmployer {

  public abstract Destination getDestination ();

  public abstract String getMessageSelector ();
}
