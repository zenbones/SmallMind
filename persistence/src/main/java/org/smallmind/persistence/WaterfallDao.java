package org.smallmind.persistence;

public interface WaterfallDao<I extends Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

   public VectoredDao<I, D> getNextDao ();
}
