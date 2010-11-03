package org.smallmind.quorum.pool.event;

import java.util.EventListener;

public interface ConnectionPoolEventListener extends EventListener {

   public abstract void connectionErrorOccurred (ErrorReportingConnectionPoolEvent event);

   public abstract void connectionLeaseTime (LeaseTimeReportingConnectionPoolEvent event);
}
