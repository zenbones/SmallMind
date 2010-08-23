package org.smallmind.seda;

public interface StageFactory<I extends Event, O extends Event> {

   public abstract Stage<I, O> assembleStage ();
}
