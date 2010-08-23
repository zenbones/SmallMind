package org.smallmind.seda;

import java.util.concurrent.TimeUnit;

public class StageController<I extends Event, O extends Event> {

   private StageFactory<I, O> stageFactory;
   private StageThreadPool<I, O> stageThreadPool;
   private EventQueue<I, O> eventQueue;
   private TimeUnit maxIdleTimeUnit;
   private TimeUnit pollTimeUnit;
   private long maxIdleTime;
   private long pollTimeout;

   public StageController (StageFactory<I, O> stageFactory, int queueCapacity, long maxIdleTime, TimeUnit maxIdleTimeUnit, long pollTimeout, TimeUnit pollTimeUnit) {

      this.stageFactory = stageFactory;
      this.maxIdleTime = maxIdleTime;
      this.maxIdleTimeUnit = maxIdleTimeUnit;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;

      eventQueue = new EventQueue<I, O>(this, queueCapacity);
      stageThreadPool = new StageThreadPool<I, O>(this);
   }

   protected EventProcessor<I, O> createEventProcessor () {

      return new EventProcessor<I, O>(this, maxIdleTime, maxIdleTimeUnit);
   }

   protected I poll ()
      throws InterruptedException {

      return eventQueue.poll(pollTimeout, pollTimeUnit);
   }

   protected void remove (EventProcessor<I, O> eventProcessor) {

      stageThreadPool.remove(eventProcessor);
   }
}
