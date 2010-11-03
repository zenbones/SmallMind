package org.smallmind.quorum.pool.event;

import java.util.EventObject;
import org.smallmind.quorum.pool.ConnectionInstance;

public class ConnectionInstanceEvent extends EventObject {

   private Exception exception;

   public ConnectionInstanceEvent (ConnectionInstance connectionInstance, Exception exception) {

      super(connectionInstance);

      this.exception = exception;
   }

   public boolean containsException () {

      return exception != null;
   }

   public Exception getException () {

      return exception;
   }
}