package org.smallmind.quorum.pool.remote;

import org.smallmind.quorum.pool.ConnectionPoolSurface;
import org.smallmind.quorum.pool.event.ConnectionPoolEventListener;

public interface RemoteConnectionPoolSurface extends ConnectionPoolSurface {

   public abstract void addConnectionPoolEventListener (ConnectionPoolEventListener listener);

   public abstract void removeConnectionPoolEventListener (ConnectionPoolEventListener listener);
}
