package org.smallmind.quorum.pool.event;

import java.util.EventObject;
import org.smallmind.quorum.pool.ConnectionPool;

public abstract class ConnectionPoolEvent extends EventObject {

   public ConnectionPoolEvent (ConnectionPool connectionPool) {

      super(connectionPool);
   }
}
