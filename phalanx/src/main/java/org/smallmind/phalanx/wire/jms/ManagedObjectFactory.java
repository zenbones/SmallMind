package org.smallmind.phalanx.wire.jms;

import javax.jms.Connection;
import javax.jms.Destination;
import org.smallmind.phalanx.wire.TransportException;

public interface ManagedObjectFactory {

  public abstract Connection createConnection ()
    throws TransportException;

  public abstract Destination getDestination ()
    throws TransportException;
}