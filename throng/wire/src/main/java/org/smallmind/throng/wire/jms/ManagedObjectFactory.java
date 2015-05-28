package org.smallmind.throng.wire.jms;

import javax.jms.Connection;
import javax.jms.Destination;
import org.smallmind.throng.wire.TransportException;

public interface ManagedObjectFactory {

  public abstract Connection createConnection ()
    throws TransportException;

  public abstract Destination getDestination ()
    throws TransportException;
}