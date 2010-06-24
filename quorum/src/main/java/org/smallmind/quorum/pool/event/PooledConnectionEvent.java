package org.smallmind.quorum.pool.event;

import java.util.EventObject;

public class PooledConnectionEvent extends EventObject {

   public PooledConnectionEvent (Object source) {

      super(source);
   }
}
