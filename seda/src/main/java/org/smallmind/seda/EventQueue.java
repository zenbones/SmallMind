package org.smallmind.seda;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class EventQueue<I extends Event, O extends Event> {

   private StageController<I, O> stageController;
   private LinkedBlockingQueue<I> internalQueue;

   public EventQueue (StageController<I, O> stageController, int capacity) {

      this.stageController = stageController;

      internalQueue = new LinkedBlockingQueue<I>(capacity);
   }

   protected void put (I event)
      throws InterruptedException {

      internalQueue.put(event);
   }

   protected I poll (long timeout, TimeUnit unit)
      throws InterruptedException {

      return internalQueue.poll(timeout, unit);
   }
}
