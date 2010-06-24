package org.smallmind.quorum.pool.event;

import java.util.EventListener;

public interface PooledConnectionListener extends EventListener {

   public abstract void pooledConnectionClosed (PooledConnectionEvent event);

   public abstract void pooledConnectionAborted (PooledConnectionEvent event);
}
