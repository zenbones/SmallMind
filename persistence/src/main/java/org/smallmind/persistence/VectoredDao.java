package org.smallmind.persistence;

public interface VectoredDao<I, D extends Durable<I>> extends Dao<I, D> {

   public abstract void updateInVector (VectorKey<I, D> vectorKey, D durable);

   public abstract void removeFromVector (VectorKey<I, D> vectorKey, D durable);

   public abstract DurableVector<I, D> getVector (VectorKey<I, D> vectorKey);

   public abstract DurableVector<I, D> persistVector (VectorKey<I, D> vectorKey, Iterable<D> elements);

   public abstract void deleteVector (VectorKey<I, D> vectorKey);
}