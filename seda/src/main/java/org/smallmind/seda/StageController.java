package org.smallmind.seda;

public class StageController<I extends Event, O extends Event> {

   private StageFactory<I, O> stageFactory;
   private ThreadPool<I, O> threadPool;
   private EventQueue<I> eventQueue;

   public StageController (StageFactory<I, O> stageFactory, SedaConfiguration sedaConfiguration) {

      this.stageFactory = stageFactory;

      eventQueue = new EventQueue<I>(sedaConfiguration.getMaxQueueCapacity());
      threadPool = new ThreadPool<I, O>(eventQueue, sedaConfiguration);
   }
}
