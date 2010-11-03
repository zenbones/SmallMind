package org.smallmind.quorum.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.quorum.pool.event.ConnectionInstanceEvent;
import org.smallmind.quorum.pool.event.ConnectionInstanceEventListener;

public abstract class AbstractConnectionInstance implements ConnectionInstance {

   private ConcurrentLinkedQueue<ConnectionInstanceEventListener> connectionInstanceEventListenerQueue;

   public AbstractConnectionInstance () {

      connectionInstanceEventListenerQueue = new ConcurrentLinkedQueue<ConnectionInstanceEventListener>();
   }

   public void addConnectionInstanceEventListener (ConnectionInstanceEventListener listener) {

      connectionInstanceEventListenerQueue.add(listener);
   }

   public void removeConnectionInstanceEventListener (ConnectionInstanceEventListener listener) {

      connectionInstanceEventListenerQueue.remove(listener);
   }

   public void fireConnectionErrorOccurred (Exception exception) {

      ConnectionInstanceEvent event = new ConnectionInstanceEvent(this, exception);

      for (ConnectionInstanceEventListener listener : connectionInstanceEventListenerQueue) {
         listener.connectionErrorOccurred(event);
      }
   }
}
