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

   public StageController (StageFactory<I, O> stageFactory, int maxQueueCapacity, double expansionFactor, int minPoolSize, int maxPoolSize, long maxIdleTime, TimeUnit maxIdleTimeUnit, long pollTimeout, TimeUnit pollTimeUnit) {

      this.stageFactory = stageFactory;
      this.maxIdleTime = maxIdleTime;
      this.maxIdleTimeUnit = maxIdleTimeUnit;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;

      eventQueue = new EventQueue<I, O>(this, maxQueueCapacity, expansionFactor);
      stageThreadPool = new StageThreadPool<I, O>(this, minPoolSize, maxPoolSize);
   }

   protected EventProcessor<I, O> createEventProcessor () {

      return new EventProcessor<I, O>(this, maxIdleTime, maxIdleTimeUnit);
   }

   protected I pollQueue ()
      throws InterruptedException {

      return eventQueue.poll(pollTimeout, pollTimeUnit);
   }

   protected synchronized boolean increasePool () {

      return stageThreadPool.increase();
   }

   protected void decreasePool (EventProcessor<I, O> eventProcessor) {

      stageThreadPool.decrease(eventProcessor);
   }
}
