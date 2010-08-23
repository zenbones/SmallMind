package org.smallmind.seda;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventQueue<I extends Event> {

   private LinkedBlockingQueue<I> internalQueue;

   public EventQueue (int capacity) {

      internalQueue = new LinkedBlockingQueue<I>(capacity);
   }

   public void put (I event)
      throws InterruptedException {

      internalQueue.put(event);
   }

   public I poll (long timeout, TimeUnit unit)
      throws InterruptedException {

      return internalQueue.poll(timeout, unit);
   }
}
