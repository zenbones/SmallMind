package org.smallmind.seda;

import java.util.concurrent.TimeUnit;

public class StageController<I extends Event, O extends Event> {

   private StageFactory<I, O> stageFactory;
   private ThreadPool<I, O> threadPool;
   private EventQueue<I> eventQueue;
   private TimeUnit maxIdleTimeUnit;
   private TimeUnit pollTimeUnit;
   private long maxIdleTime;
   private long pollTimeout;

   public StageController (StageFactory<I, O> stageFactory, int maxQueueCapacity, int minPoolSize, int maxPoolSize, long pollTimeout, TimeUnit pollTimeUnit, long trackingTime, TimeUnit trackingTimeUnit, long monitorPulseTime, TimeUnit monitorPulseTimeUnit) {

      this.stageFactory = stageFactory;
      this.pollTimeout = pollTimeout;
      this.pollTimeUnit = pollTimeUnit;

      eventQueue = new EventQueue<I>(maxQueueCapacity);
      threadPool = new ThreadPool<I, O>(eventQueue, minPoolSize, maxPoolSize, pollTimeout, pollTimeUnit, trackingTime, trackingTimeUnit, monitorPulseTime, monitorPulseTimeUnit);
   }
}
