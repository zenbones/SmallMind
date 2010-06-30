package org.smallmind.persistence;

import java.util.Comparator;
import org.smallmind.persistence.cache.DurableVector;

public interface VectoredDao<I extends Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

   public abstract void updateInVector (VectorKey<D> vectorKey, D durable);

   public abstract void removeFromVector (VectorKey<D> vectorKey, D durable);

   public abstract DurableVector<I, D> getVector (VectorKey<D> vectorKey);

   public abstract DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector);

   public abstract DurableVector<I, D> migrateVector (DurableVector<I, D> vector);

   public abstract DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, long timeToLive);

   public abstract DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered);

   public abstract void deleteVector (VectorKey<D> vectorKey);
}